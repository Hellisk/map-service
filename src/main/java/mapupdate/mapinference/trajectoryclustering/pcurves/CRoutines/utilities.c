#include "jni.h"
#include "Utilities_MyMath.h"
#include <string.h>
#include <stdlib.h>

/* C routines */

#define SWAP(a,b) temp=(a);(a)=(b);(b)=temp;

void *my_malloc(long size) {
    void *ptr;
    ptr=(void *)malloc(size);
    if(!ptr) { 
	printf("Out of memory!\n"); 
	exit (1); 
    }
    return(ptr);
}

#define INSERTION_SORT_THRESHOLD 7
#define STACK_SIZE 50

void indexArray(double arr[],long in[],long n) {
    /* Quicksort from Numerical Recipes in C, Chapter 8.2 */
    long aI,i,ir=n,j,k,l=1,*istack,*istackTemp,temp;
    int stackSize = STACK_SIZE;
    int jstack = 0,js;
    double a;

    istack = my_malloc(sizeof(long)*stackSize);

    for(i=0;i<n;i++)
	in[i] = i;

    while(1) {
	if (ir - l < INSERTION_SORT_THRESHOLD) {
	    for(j=l;j<ir;j++) {
		a = arr[in[j]];
		aI = in[j];
		for(i=j-1;i>=0;i--) {
		    if (arr[in[i]] <= a)
			break;
		    in[i+1] = in[i];
		}
		in[i+1] = aI;
	    }
	    if (jstack == 0)
		break;
	    ir = istack[jstack--];
	    l = istack[jstack--];
	}
	else {
	    k = (l+ir) >> 1;
	    SWAP(in[k-1],in[l]);
	    if (arr[in[l-1]] > arr[in[ir-1]]) {
		SWAP(in[l-1],in[ir-1]);
	    }
	    if (arr[in[l]] > arr[in[ir-1]]) {
		SWAP(in[l],in[ir-1]);
	    }
	    if (arr[in[l-1]] > arr[in[l]]) {
		SWAP(in[l-1],in[l]);
	    }
	    i = l+1;
	    j = ir;
	    a = arr[in[l]];
	    aI = in[l];
	    while(1) {
		do i++; while (arr[in[i-1]] < a);
		do j--; while (arr[in[j-1]] > a);
		if (j < i)
		    break;
		SWAP(in[i-1],in[j-1]);
	    }
	    in[l] = in[j-1];
	    in[j-1] = aI;
	    jstack += 2;
	    if (jstack > stackSize) {
		istackTemp = my_malloc(sizeof(long)*stackSize*2);
		for(js=0;js<stackSize-2;js++)
		    istackTemp[js] = istack[js];
		free(istack);
		istack = istackTemp;
		stackSize *= 2;		
	    }
	    if (ir-i+1 >= j-1) {
		istack[jstack] = ir;
		istack[jstack-1] = i;
		ir = j-1;
	    }
	    else {
		istack[jstack] = j-1;
		istack[jstack-1] = l;
		l = i;
	    }
	}
    }
    free(istack);
}
		

/* Java auxiliary functions */

JNIEXPORT void JNICALL Java_Utilities_MyMath_IndexArrayC
(JNIEnv *env,jclass class,jdoubleArray jArray,jintArray jIndex,jint size) {
    jdouble *arr = (*env)->GetDoubleArrayElements(env,jArray,0);
    jint *in = (*env)->GetIntArrayElements(env,jIndex,0);
    jsize length = (*env)->GetArrayLength(env,jArray);
    indexArray(arr,in,length);
    (*env)->ReleaseDoubleArrayElements(env,jArray,arr,0);
    (*env)->ReleaseIntArrayElements(env,jIndex,in,0);
}


JNIEXPORT jint JNICALL Java_Utilities_MyMath_equalsC
(JNIEnv *env, jclass class, jdouble d1, jdouble d2,jdouble epsilon) {
    if (d1 == 0.0) {
	if (d2 == 0.0)
	    return 1;
	else
	    return 0;
    }
    else if (fabs(d2/d1 - 1.0) < epsilon)
	return 1;
    else
	return 0;
}

JNIEXPORT jint JNICALL Java_Utilities_MyMath_MatrixInvertC
(JNIEnv *env,jclass class,jdoubleArray jMatrix,jint size) {
    /* From Numerical Recipes in C, Chapter 2.1 */
    jdouble *matrixVector = (*env)->GetDoubleArrayElements(env,jMatrix,0);
    int i,j,k,l,ll;                 
    int *ipiv = my_malloc(sizeof(int)*size);
    int *indxc = my_malloc(sizeof(int)*size);
    int *indxr = my_malloc(sizeof(int)*size);
    int icol = -1,irow = -1;
    double big,dum,pivinv,temp;
    jclass exception;

    for(j=0;j<size;j++)
	ipiv[j] = 0;
    for(i=0;i<size;i++) {
	big = 0.0;
	for(j=0;j<size;j++) {
	    if (ipiv[j] != 1) {
		for(k=0;k<size;k++) {
		    if (ipiv[k] == 0) {
			if (fabs(matrixVector[j*size + k]) >= big) {
			    big = fabs(matrixVector[j*size + k]);
			    irow = j;
			    icol = k;
			}
		    }
		    else if (ipiv[k] > 1) {
			free(ipiv);
			free(indxc);
			free(indxr);
			(*env)->ReleaseDoubleArrayElements(env,jMatrix,matrixVector,0);
			return -1;
		    }
		}
	    }
	}
	ipiv[icol]++;
	if (irow != icol) {
	    for(l=0;l<size;l++) {
		temp = matrixVector[irow*size + l];
		matrixVector[irow*size + l] = matrixVector[icol*size + l];
		matrixVector[icol*size + l] = temp;
	    }
	}
	indxr[i] = irow;
	indxc[i] = icol;
	if (matrixVector[icol*size + icol] == 0.0) {
	    free(ipiv);
	    free(indxc);
	    free(indxr);
	    (*env)->ReleaseDoubleArrayElements(env,jMatrix,matrixVector,0);
	    return -2;
	}
	pivinv = 1.0/matrixVector[icol*size + icol];
	matrixVector[icol*size + icol] = 1.0;
	for(l=0;l<size;l++)
	    matrixVector[icol*size + l] *= pivinv;
	for(ll=0;ll<size;ll++) {
	    if (ll != icol) {
		dum = matrixVector[ll*size + icol];
		matrixVector[ll*size + icol] = 0.0;
		for(l=0;l<size;l++)
		    matrixVector[ll*size + l] -= matrixVector[icol*size + l] * dum;
	    }
	}
    }
    for(l=size-1;l>=0;l--) {
	if (indxr[l] != indxc[l]) {
	    for(k=0;k<size;k++) {
		temp = matrixVector[k*size + indxr[l]];
		matrixVector[k*size + indxr[l]] = matrixVector[k*size + indxc[l]];
		matrixVector[k*size + indxc[l]] = temp;
	    }
	}
    }
    free(ipiv);
    free(indxc);
    free(indxr);
    (*env)->ReleaseDoubleArrayElements(env,jMatrix,matrixVector,0);
    return 0;
}
