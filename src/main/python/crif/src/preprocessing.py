import pandas as pd


# import crash_on_ipy


def haversine(lon1, lat1, lon2, lat2):
    from math import radians, cos, sin, asin, sqrt
    """
    Calculate the great circle distance between two points
    on the earth (specified in decimal degrees)
    http://boulter.com/gps/distance/
    """
    # convert decimal degrees to radians
    lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])
    # haversine formula
    dlon = lon2 - lon1
    dlat = lat2 - lat1
    a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
    c = 2 * asin(sqrt(a))
    m = 6367000.0 * c
    return m


def over_speed(d, max_speed):
    # print d.x
    delt_dist = map(lambda x: haversine(x[0], x[1], x[2], x[3]), zip(d.x[1:], d.y[1:], d.x[:-1], d.y[:-1]))
    dt = pd.to_datetime(d.time)
    s = map(lambda x: x.total_seconds(), np.subtract(dt[1:], dt[:-1]))
    df = pd.DataFrame(zip(range(len(s)), s, delt_dist, np.subtract(d.tid[1:], d.tid[:-1])))
    df['speed'] = np.divide(df[2], df[1])
    df = df.fillna(0)
    # df = df.drop(df[df.speed > max_speed].index)
    # return df
    idx = df[(df.speed > max_speed) & (df[3] == 0)].index + 1
    i = 0
    drop_idx = []
    while i < len(idx):
        if i + 1 >= len(idx) or idx[i + 1] - idx[i] > 5:
            drop_idx.append(idx[i])
        else:
            drop_idx += range(idx[i], idx[i + 1])
            i += 1
        i += 1
    print 'over_speed', len(drop_idx)
    return drop_idx


def under_speed(d, min_speed):
    delt_dist = map(lambda x: haversine(x[0], x[1], x[2], x[3]), zip(d.x[1:], d.y[1:], d.x[:-1], d.y[:-1]))
    dt = pd.to_datetime(d.time)
    s = map(lambda x: x.total_seconds(), np.subtract(dt[1:], dt[:-1]))
    df = pd.DataFrame(zip(range(len(s)), s, delt_dist, np.subtract(d.tid[1:], d.tid[:-1])))
    df['speed'] = np.divide(df[2], df[1])
    df = df.fillna(0)
    # return df
    # df = df.drop(df[df.speed > max_speed].index)
    # return df
    idx = df[(df.speed < min_speed) & (df[3] == 0)].index + 1
    i = 0
    drop_idx = []
    while i < len(idx):
        if i + 1 >= len(idx) or idx[i + 1] - idx[i] > 5:
            drop_idx.append(idx[i])
        else:
            drop_idx += range(idx[i], idx[i + 1] + 1)
            i += 1
        i += 1
    print '\tunder_speed', len(drop_idx)
    '''
    if len(drop_idx) < 50:
        print drop_idx
        if len(drop_idx) < 30:
            return []
    '''
    return drop_idx


def split(d, minutes):
    d.index = range(1, len(d) + 1)
    d.time = pd.to_datetime(d.time)
    delta_time = np.subtract(d.time[1:], d.time[:-1])
    delta_id = np.subtract(d.tid[1:], d.tid[:-1])
    split_id = delta_id[delta_id != 0].index
    split_time = delta_time[delta_time > np.timedelta64(minutes, 'm')].index
    split_all = pd.Series([1] + list(split_id) + list(split_time) + [len(d)]).unique()
    split_all.sort()
    window = np.subtract(split_all[1:], split_all[:-1])
    if len(window) == 0:
        print 'hi'
        return pd.DataFrame()
    window[-1] += 1
    l_id = []
    idx = 1
    for i in window:
        l_id += [idx] * i
        idx += 1
    # print d
    # assert len(d) == sum(window)
    d['tid'] = l_id
    return d


def trim(d, MAX_SPEED=60, MIN_SPEED=0.01, minutes=5):
    # dt = d.time.copy()
    # dt.sort()
    # d = d.ix[dt.index]
    # print d
    d = d.sort(['tid', 'time'])
    d = split(d, minutes)
    d.index = range(len(d))
    drop_idx = over_speed(d, MAX_SPEED) + under_speed(d, MIN_SPEED)
    while len(drop_idx) != 0 and len(d) > 2:
        # print '\tDrop:', len(drop_idx), 'D:', len(d)
        d = d.drop(drop_idx)
        d = split(d, minutes)
        d.index = range(len(d))
        # drop_idx = over_speed(d, MAX_SPEED)+under_speed(d, MIN_SPEED)
        drop_idx = under_speed(d, MIN_SPEED)
    return d
