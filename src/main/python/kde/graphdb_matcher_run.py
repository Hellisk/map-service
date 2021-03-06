import math

import spatialfunclib
from graphdb_matcher import GraphDBMatcher

MAX_SPEED_M_PER_S = 20.0


class MatchGraphDB:
    def __init__(self, graphdb_filename, constraint_length, max_dist):
        self.matcher = GraphDBMatcher(graphdb_filename, constraint_length, max_dist)
        self.constraint_length = constraint_length

    def process_trip(self, trip_directory, trip_filename, output_directory):

        trip_file = open(trip_directory + "/" + trip_filename, 'r')
        raw_observations = map(lambda x: x.strip("\n").split(' ')[0:3], trip_file.readlines())
        trip_file.close()

        v = None
        p = {}

        obs = []
        obs_states = []
        max_prob_p = None

        (first_obs_lon, first_obs_lat, first_obs_time) = raw_observations[0]

        (v, p) = self.matcher.step((float(first_obs_lat), float(first_obs_lon)), v, p)

        max_prob_state = max(v, key=lambda x: v[x])
        max_prob_p = p[max_prob_state]

        if len(max_prob_p) == self.constraint_length:
            obs_states.append(max_prob_p[0])

        obs.append((first_obs_lat, first_obs_lon, first_obs_time))

        for index in range(1, len(raw_observations)):
            (prev_lon, prev_lat, prev_time) = raw_observations[index - 1]
            (curr_lon, curr_lat, curr_time) = raw_observations[index]

            prev_time = float(prev_time)
            curr_time = float(curr_time)

            elapsed_time = (curr_time - prev_time)

            distance = spatialfunclib.distance((float(prev_lat), float(prev_lon)), (float(curr_lat), float(curr_lon)))

            if distance > 10.0:
                int_steps = int(math.ceil(distance / 10.0))
                int_step_distance = (distance / float(int_steps))
                int_step_time = (float(elapsed_time) / float(int_steps))

                for j in range(1, int_steps):
                    step_fraction_along = ((j * int_step_distance) / distance)
                    (int_step_lat, int_step_lon) = spatialfunclib.point_along_line(float(prev_lat), float(prev_lon),
                                                                                   float(curr_lat), float(curr_lon),
                                                                                   step_fraction_along)

                    (v, p) = self.matcher.step((float(int_step_lat), float(int_step_lon)), v, p)

                    max_prob_state = max(v, key=lambda x: v[x])
                    max_prob_p = p[max_prob_state]

                    if (len(max_prob_p) == self.constraint_length):
                        obs_states.append(max_prob_p[0])

                    obs.append((int_step_lat, int_step_lon, (float(prev_time) + (float(j) * int_step_time))))

            (v, p) = self.matcher.step((float(curr_lat), float(curr_lon)), v, p)

            max_prob_state = max(v, key=lambda x: v[x])
            max_prob_p = p[max_prob_state]

            if len(max_prob_p) == self.constraint_length:
                obs_states.append(max_prob_p[0])

            obs.append((curr_lat, curr_lon, curr_time))

        if (len(max_prob_p) < self.constraint_length):
            obs_states.extend(max_prob_p)
        else:
            obs_states.extend(max_prob_p[1:])

        # print "obs: " + str(len(obs))
        # print "obs states: " + str(len(obs_states))
        assert (len(obs_states) == len(obs))

        out_file = open(output_directory + "/matched_" + trip_filename, 'w')

        for index in range(0, len(obs)):
            (obs_lat, obs_lon, obs_time) = obs[index]
            out_file.write(str(obs_lat) + " " + str(obs_lon) + " " + str(obs_time) + " ")

            if (obs_states[index] == "unknown"):
                out_file.write(str(obs_states[index]) + "\n")
            else:
                (in_node_coords, out_node_coords) = obs_states[index]
                out_file.write(
                    str(in_node_coords[0]) + " " + str(in_node_coords[1]) + " " + str(out_node_coords[0]) + " " + str(
                        out_node_coords[1]) + "\n")

        out_file.close()


import sys, getopt
import os

if __name__ == '__main__':

    cache_folder = ""
    input_folder = ""  # the current folder should not exist, replaced by -t argument
    temp_graphdb_filename = "skeleton_maps/skeleton_map_1m.db"
    temp_output_directory = "matched_trips_1m/"

    (opts, args) = getopt.getopt(sys.argv[1:], "f:c:m:d:t:o:h")
    for o, a in opts:
        if o == "-f":
            cache_folder = str(a)
        elif o == "-c":
            constraint_length = int(a)
        elif o == "-m":
            max_dist = int(a)
        elif o == "-d":
            temp_graphdb_filename = str(a)
        elif o == "-t":
            input_folder = str(a)
        elif o == "-o":
            temp_output_directory = str(a)
        elif o == "-h":
            print "Usage: python graphdb_matcher_run.py [-c <constraint_length>] [-m <max_dist>] [-d " \
                  "<graphdb_filename>] [-t <trip_directory>] [-f <cache_folder>] [-o <output_directory>] [-h] "
            exit()

    graphdb_filename = cache_folder + temp_graphdb_filename
    output_directory = cache_folder + temp_output_directory
    constraint_length = 10
    max_dist = 100
    # print "constraint length: " + str(constraint_length)
    # print "max dist: " + str(max_dist)
    # print "graphdb filename: " + str(graphdb_filename)
    # print "trip directory: " + str(trip_directory)
    # print "output directory: " + str(output_directory)

    # create match result directory
    if not os.path.exists(output_directory):
        # create trips directory
        os.mkdir(output_directory)

    match_graphdb = MatchGraphDB(graphdb_filename, constraint_length, max_dist)

    all_trip_files = filter(lambda x: x.startswith("trip_") and x.endswith(".txt"), os.listdir(input_folder))

    for i in range(0, len(all_trip_files)):
        sys.stdout.write("\rProcessing trip " + str(i + 1) + "/" + str(len(all_trip_files)) + "... ")
        sys.stdout.flush()

        match_graphdb.process_trip(input_folder, all_trip_files[i], output_directory)

    # sys.stdout.write("done.\n")
    sys.stdout.flush()
