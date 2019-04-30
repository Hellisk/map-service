#include "jni.h"
#include "LinearAlgebra_VektorDD.h"
#include <string.h>
#include <stdlib.h>

JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_InitializeCoordsC
(JNIEnv *env,jobject obj,jdoubleArray jCoords) {
    jdouble *coords = (*env)->GetDoubleArrayElements(env,jCoords,0);
    jsize length = (*env)->GetArrayLength(env,jCoords);
    int i;
    for(i=0;i<length;i++)
	coords[i] = 0;
    (*env)->ReleaseDoubleArrayElements(env,jCoords,coords,0);
}

JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_UpdateC
(JNIEnv *env,jobject obj,jdoubleArray jCoords1,jdoubleArray jCoords2) {
    jdouble *coords1 = (*env)->GetDoubleArrayElements(env,jCoords1,0);
    jdouble *coords2 = (*env)->GetDoubleArrayElements(env,jCoords2,0);
    jsize length = (*env)->GetArrayLength(env,jCoords1);
    int i;
    for(i=0;i<length;i++)
	coords1[i] = coords2[i];
    (*env)->ReleaseDoubleArrayElements(env,jCoords1,coords1,0);
    (*env)->ReleaseDoubleArrayElements(env,jCoords2,coords2,0);
}

JNIEXPORT jdouble JNICALL Java_LinearAlgebra_VektorDD_MulVektorialC
(JNIEnv *env,jobject obj,jdoubleArray jCoords1,jdoubleArray jCoords2) {
    jdouble *coords1 = (*env)->GetDoubleArrayElements(env,jCoords1,0);
    jdouble *coords2 = (*env)->GetDoubleArrayElements(env,jCoords2,0);
    jsize length = (*env)->GetArrayLength(env,jCoords1);
    int i;
    double d = 0;
    for(i=0;i<length;i++)
	d += coords1[i] * coords2[i];
    (*env)->ReleaseDoubleArrayElements(env,jCoords1,coords1,0);
    (*env)->ReleaseDoubleArrayElements(env,jCoords2,coords2,0);
    return d;
}

JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_AddEqualC
(JNIEnv *env,jobject obj,jdoubleArray jCoords1,jdoubleArray jCoords2) {
    jdouble *coords1 = (*env)->GetDoubleArrayElements(env,jCoords1,0);
    jdouble *coords2 = (*env)->GetDoubleArrayElements(env,jCoords2,0);
    jsize length = (*env)->GetArrayLength(env,jCoords1);
    int i;
    for(i=0;i<length;i++)
	coords1[i] += coords2[i];
    (*env)->ReleaseDoubleArrayElements(env,jCoords1,coords1,0);
    (*env)->ReleaseDoubleArrayElements(env,jCoords2,coords2,0);
}

JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_SubEqualC
(JNIEnv *env,jobject obj,jdoubleArray jCoords1,jdoubleArray jCoords2) {
    jdouble *coords1 = (*env)->GetDoubleArrayElements(env,jCoords1,0);
    jdouble *coords2 = (*env)->GetDoubleArrayElements(env,jCoords2,0);
    jsize length = (*env)->GetArrayLength(env,jCoords1);
    int i;
    for(i=0;i<length;i++)
	coords1[i] -= coords2[i];
    (*env)->ReleaseDoubleArrayElements(env,jCoords1,coords1,0);
    (*env)->ReleaseDoubleArrayElements(env,jCoords2,coords2,0);
}

JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_MulEqualC
(JNIEnv *env,jobject obj,jdoubleArray jCoords,jdouble d) {
    jdouble *coords = (*env)->GetDoubleArrayElements(env,jCoords,0);
    jsize length = (*env)->GetArrayLength(env,jCoords);
    int i;
    for(i=0;i<length;i++)
	coords[i] *= d;
    (*env)->ReleaseDoubleArrayElements(env,jCoords,coords,0);
}

JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_DivEqualC
(JNIEnv *env,jobject obj,jdoubleArray jCoords,jdouble d) {
    jdouble *coords = (*env)->GetDoubleArrayElements(env,jCoords,0);
    jsize length = (*env)->GetArrayLength(env,jCoords);
    int i;
    for(i=0;i<length;i++)
	coords[i] /= d;
    (*env)->ReleaseDoubleArrayElements(env,jCoords,coords,0);
}

JNIEXPORT jdouble JNICALL Java_LinearAlgebra_VektorDD_Norm2C
(JNIEnv *env,jobject obj,jdoubleArray jCoords) {
    jdouble *coords = (*env)->GetDoubleArrayElements(env,jCoords,0);
    jsize length = (*env)->GetArrayLength(env,jCoords);
    int i;
    double d = 0;
    for(i=0;i<length;i++)
	d += coords[i] * coords[i];
    (*env)->ReleaseDoubleArrayElements(env,jCoords,coords,0);
    return sqrt(d);
}

JNIEXPORT jdouble JNICALL Java_LinearAlgebra_VektorDD_Norm2SquaredC
(JNIEnv *env,jobject obj,jdoubleArray jCoords) {
    jdouble *coords = (*env)->GetDoubleArrayElements(env,jCoords,0);
    jsize length = (*env)->GetArrayLength(env,jCoords);
    int i;
    double d = 0;
    for(i=0;i<length;i++)
	d += coords[i] * coords[i];
    (*env)->ReleaseDoubleArrayElements(env,jCoords,coords,0);
    return d;
}

JNIEXPORT jdouble JNICALL Java_LinearAlgebra_VektorDD_Dist2C
(JNIEnv *env,jobject obj,jdoubleArray jCoords1, jdoubleArray jCoords2) {
    jdouble *coords1 = (*env)->GetDoubleArrayElements(env,jCoords1,0);
    jdouble *coords2 = (*env)->GetDoubleArrayElements(env,jCoords2,0);
    jsize length = (*env)->GetArrayLength(env,jCoords1);
    int i;
    double d = 0,c;
    for(i=0;i<length;i++)
	d += ((c = coords1[i] - coords2[i])) * c;
    (*env)->ReleaseDoubleArrayElements(env,jCoords1,coords1,0);
    (*env)->ReleaseDoubleArrayElements(env,jCoords2,coords2,0);
    return sqrt(d);
}

JNIEXPORT jdouble JNICALL Java_LinearAlgebra_VektorDD_Dist2SquaredC
(JNIEnv *env,jobject obj,jdoubleArray jCoords1, jdoubleArray jCoords2) {
    jdouble *coords1 = (*env)->GetDoubleArrayElements(env,jCoords1,0);
    jdouble *coords2 = (*env)->GetDoubleArrayElements(env,jCoords2,0);
    jsize length = (*env)->GetArrayLength(env,jCoords1);
    int i;
    double d = 0,c;
    for(i=0;i<length;i++)
	d += ((c = coords1[i] - coords2[i])) * c;
    (*env)->ReleaseDoubleArrayElements(env,jCoords1,coords1,0);
    (*env)->ReleaseDoubleArrayElements(env,jCoords2,coords2,0);
    return d;
}

JNIEXPORT jdouble JNICALL Java_LinearAlgebra_Vektor2D_Dist2LineSegmentC
(JNIEnv *env,jobject obj,jdouble coordX,jdouble coordY,jdouble coordX1,jdouble coordY1,
 jdouble coordX2,jdouble coordY2) {
    double pX = coordX1 - coordX2;
    double pY = coordY1 - coordY2;
    double p1X = coordX - coordX1;
    double p1Y = coordY - coordY1;
    double p2X = coordX - coordX2;
    double p2Y = coordY - coordY2;
    double d,da,dX,dY;
    
    if ( (d = pX*p1X + pY*p1Y) >= 0) 
	return sqrt(p1X * p1X + p1Y * p1Y);
    
    if ( pX*p2X + pY*p2Y <= 0)
	return sqrt(p2X * p2X + p2Y * p2Y);
    
    da = d/(pX*pX + pY*pY);
    dX = p1X - da*pX;
    dY = p1Y - da*pY;
    return sqrt(dX * dX + dY * dY);
}
