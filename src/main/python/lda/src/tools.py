import os
import random

import numpy as np
import pandas as pd
import sys


def sample(data, k):
    tids = data.tid.unique()
    df = pd.DataFrame()
    i = 0
    for tid in random.sample(tids, k):
        _df = data[data.tid == tid].copy()
        _df.tid = i
        df = pd.concat([df, _df])
        i += 1
    return df


def to_biagioni(data, to_folder, x='lon', y='lat'):
    print os.system('mkdir %s' % (to_folder))
    trip_counter = 0
    data['pid'] = range(1, len(data) + 1)
    data['timestamp'] = (data.time.values - np.datetime64('1970-01-01T00:00:00Z')) / np.timedelta64(1, 's')
    if not to_folder.endswith('/'):
        to_folder += '/'
    for tid in data.tid.unique():
        file_name = "trip_%d.txt" % (tid)
        # print to_folder+file_name
        f = open(to_folder + file_name, 'w')
        d = data[data.tid == tid]
        i = 0
        for idx, row in d.iterrows():
            if ((trip_counter % 100 == 0) or (trip_counter == len(data))):
                sys.stdout.write("\rCompleted: " + str(trip_counter) + "/" + str(len(data)) + "... ")
                sys.stdout.flush()
            if i == 0:
                f.write("%d,%f,%f,%.1f,%s,%d\n" % (row.pid, row[y], row[x], row.timestamp, 'None', row.pid + 1))
            elif i == len(d) - 1:
                f.write("%d,%f,%f,%.1f,%d,%s\n" % (row.pid, row[y], row[x], row.timestamp, row.pid - 1, 'None'))
            else:
                f.write("%d,%f,%f,%.1f,%d,%d\n" % (row.pid, row[y], row[x], row.timestamp, row.pid - 1, row.pid + 1))
            i += 1
            trip_counter += 1


def to_ahmed(data, to_folder, x='x', y='y', time='t'):
    print os.system('mkdir %s' % (to_folder))
    trip_counter = 0
    data['pid'] = range(1, len(data) + 1)
    data[time] = (data.time.values - np.datetime64('1970-01-01T00:00:00Z')) / np.timedelta64(1, 's')
    if not to_folder.endswith('/'):
        to_folder += '/'
    for tid in data.tid.unique():
        file_name = "trip_%d.txt" % (tid)
        # print to_folder+file_name
        f = open(to_folder + file_name, 'w')
        d = data[data.tid == tid]
        i = 0
        for idx, row in d.iterrows():
            if ((trip_counter % 100 == 0) or (trip_counter == len(data))):
                sys.stdout.write("\rCompleted: " + str(trip_counter) + "/" + str(len(data)) + "... ")
                sys.stdout.flush()
            f.write("%f %f %.1f\n" % (row[x], row[y], row.t))

            i += 1
            trip_counter += 1


if __name__ == '__main__':
    pass
