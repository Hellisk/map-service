import getopt
import os

import cv
import sys
from itertools import tee, izip

import spatialfunclib
from location import TripLoader

##
## important parameters
##

cell_size = 1  # meters
gaussian_blur = 20


def pairwise(iterable):
    """s -> (s0,s1), (s1,s2), (s2, s3), ..."""
    a, b = tee(iterable)
    next(b, None)
    return izip(a, b)


class KDE:
    def __init__(self):
        pass

    def create_kde_with_trips(self, all_trips):

        # print "trips path: " + str(input_path)
        # print "cell size: " + str(cell_size)
        # print "gaussian blur: " + str(gaussian_blur)

        sys.stdout.write("\nFinding bounding box... ")
        sys.stdout.flush()

        min_lat = all_trips[0].locations[0].latitude
        max_lat = all_trips[0].locations[0].latitude
        min_lon = all_trips[0].locations[0].longitude
        max_lon = all_trips[0].locations[0].longitude

        for trip in all_trips:
            for location in trip.locations:
                if location.latitude < min_lat:
                    min_lat = location.latitude

                if location.latitude > max_lat:
                    max_lat = location.latitude

                if location.longitude < min_lon:
                    min_lon = location.longitude

                if location.longitude > max_lon:
                    max_lon = location.longitude

        print "done."

        # find bounding box for data
        min_lat -= 0.003
        max_lat += 0.003
        min_lon -= 0.005
        max_lon += 0.005

        diff_lat = max_lat - min_lat
        diff_lon = max_lon - min_lon

        # write bounding box file
        # output that we are starting the writing process
        # sys.stdout.write("\nWriting bounding box file...")
        sys.stdout.flush()

        # open bounding box file
        boundingbox_file = open(cache_folder + "bounding_box.txt", 'w')

        # output lat and lot to file
        boundingbox_file.write(str(min_lat) + " " + str(min_lon) + " " + str(max_lat) + " " + str(max_lon))

        # close bounding box file
        boundingbox_file.close()

        print "done."

        # print min_lat, min_lon, max_lat, max_lon

        width = int(diff_lon * spatialfunclib.METERS_PER_DEGREE_LONGITUDE / cell_size)
        height = int(diff_lat * spatialfunclib.METERS_PER_DEGREE_LATITUDE / cell_size)
        yscale = height / diff_lat  # pixels per lat
        xscale = width / diff_lon  # pixels per lon

        # aggregate intensity map for all traces
        themap = cv.CreateMat(height, width, cv.CV_16UC1)
        cv.SetZero(themap)

        ##
        ## Build an aggregate intensity map from all the edges
        ##

        trip_counter = 1

        for trip in all_trips:

            if ((trip_counter % 10 == 0) or (trip_counter == len(all_trips))):
                # sys.stdout.write(
                #     "\rCreating histogram (trip " + str(trip_counter) + "/" + str(len(all_trips)) + ")... ")
                sys.stdout.flush()
            trip_counter += 1

            temp = cv.CreateMat(height, width, cv.CV_8UC1)
            cv.SetZero(temp)
            temp16 = cv.CreateMat(height, width, cv.CV_16UC1)
            cv.SetZero(temp16)

            for (orig, dest) in pairwise(trip.locations):
                oy = height - int(yscale * (orig.latitude - min_lat))
                ox = int(xscale * (orig.longitude - min_lon))
                dy = height - int(yscale * (dest.latitude - min_lat))
                dx = int(xscale * (dest.longitude - min_lon))
                cv.Line(temp, (ox, oy), (dx, dy), (32), 1, cv.CV_AA)

            # accumulate trips into themap
            cv.ConvertScale(temp, temp16, 1, 0)
            cv.Add(themap, temp16, themap)

        lines = cv.CreateMat(height, width, cv.CV_8U)
        cv.SetZero(lines)

        print "done."

        trip_counter = 1

        for trip in all_trips:

            if (trip_counter % 10 == 0) or (trip_counter == len(all_trips)):
                # sys.stdout.write("\rCreating drawing (trip " + str(trip_counter) + "/" + str(len(all_trips)) + ")... ")
                sys.stdout.flush()
            trip_counter += 1

            for (orig, dest) in pairwise(trip.locations):
                oy = height - int(yscale * (orig.latitude - min_lat))
                ox = int(xscale * (orig.longitude - min_lon))
                dy = height - int(yscale * (dest.latitude - min_lat))
                dx = int(xscale * (dest.longitude - min_lon))
                cv.Line(lines, (ox, oy), (dx, dy), (255), 1, cv.CV_AA)

        # save the lines
        cv.SaveImage(cache_folder + "raw_data.png", lines)

        print "done."
        print "Intensity map acquired."
        sys.stdout.write("Smoothing... ")
        sys.stdout.flush()

        # create the mask and compute the contour
        cv.Smooth(themap, themap, cv.CV_GAUSSIAN, gaussian_blur, gaussian_blur)
        cv.SaveImage(cache_folder + "kde.png", themap)

        print "done."
        print "\nKDE generation complete."


if __name__ == '__main__':

    opts, args = getopt.getopt(sys.argv[1:], "c:b:i:f:h")
    input_path = ""
    cache_folder = ""
    for o, a in opts:
        if o == "-c":
            cell_size = int(a)
        elif o == "-b":
            gaussian_blur = int(a)
        elif o == "-i":
            input_path = str(a)
        elif o == "-f":
            cache_folder = str(a)
        elif o == "-h":
            print "Usage: kde.py [-c <cell_size>] [-b <gaussian_blur_size>] [-i <input_trips_path>] [-f <cache_folder>][-h]\n"
            sys.exit()

    # create output directory
    os.mkdir(cache_folder)

    k = KDE()
    k.create_kde_with_trips(TripLoader.load_all_trips(input_path))
