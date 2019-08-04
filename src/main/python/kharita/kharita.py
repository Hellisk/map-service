"""
author: rade
Create the road network by merging trajectories.
"""
import getopt

import sys
import time

from methods_kharita import getdata, computeclusters, coocurematrix, prunegraph, printedges

if __name__ == '__main__':
    # Default parameters
    start = time.time();
    # LL = (41, -87);
    # print(vincenty(LL, (LL[0] + 1, LL[1])).meters, vincenty(LL, (LL[0], LL[1] + 1)).meters)
    # latconst = vincenty(LL, (LL[0] + 1, LL[1])).meters;
    # lonconst = vincenty(LL, (LL[0], LL[1] + 1)).meters
    theta = 150;
    SEEDRADIUS = 100;
    datafile = ''  # 'gps_points_01-10'
    write_file = ''
    noise_percent = -1
    max_noise_radius = -1
    drawmap = False
    (opts, args) = getopt.getopt(sys.argv[1:], "f:m:p:r:s:a:d:h")
    for o, a in opts:
        if o == "-f":
            datafile = str(a)
        if o == "-r":
            SEEDRADIUS = float(a)
        if o == "-s":
            theta = float(a)
        if o == "-p":
            write_file = str(a)
        if o == "-h":
            print("Usage: python kharita.py [-f <file_name>] [-r <seerdradius>] [-s <theta]")
            exit()
    print('data:', datafile, 'theta: ', theta, 'seed radius', SEEDRADIUS)
    nsamples = 20000000;
    datapointwts = getdata(nsamples, datafile);
    print('all datapoints ', len(datapointwts))
    datapointwts = [xx for xx in datapointwts if xx[3] >= 10];  # filter low speed points
    print('datapoints with speed>=5kmph: ', len(datapointwts))
    seeds = computeclusters(datapointwts, 50, SEEDRADIUS, theta);  # compute k-means; seeds cluster centroids
    print('clusters: ', len(seeds), time.time() - start)
    gedges = coocurematrix(datapointwts, seeds, theta)  # compute connectivity graph
    print('coocurence matrix computed: ', time.time() - start)
    gedges = prunegraph(gedges, seeds);  # spanner; pruning edges
    print('graph pruning. number of edges = ', len(gedges), time.time() - start)
    printedges(gedges, seeds, datapointwts, write_file, theta)
    # plotmap(seeds, gedges, datapointwts)
    # plt.show()
