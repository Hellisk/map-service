import math as Math
import numpy as np
import pandas as pd
from math import atan, cos, pi
from sklearn.ensemble import RandomForestRegressor

rc = 6378137
rj = 6356725


# gongcan_list=['Angle','Frequency Channel',BCCH Frequency Point,NCC,BCC,TCH Available Count,CCCH Channel Count,GPRS Static Channel Count,GPRS Dinamic Channel Count,Power,Minimum Access LEVEL,TD Threshold,2G-TD CRO,CF Count,EDGE CF Count,Cover Type,Cover Scence,Auto LC Test Area,GSM Neighbor Count,TD Neighbor Count]

def rad(d):
    return d * Math.pi / 180.0


def azimuth(pt_a, pt_b):
    lon_a, lat_a = pt_a
    lon_b, lat_b = pt_b
    rlon_a, rlat_a = rad(lon_a), rad(lat_a)
    rlon_b, rlat_b = rad(lon_b), rad(lat_b)
    ec = rj + (rc - rj) * (90. - lat_a) / 90.
    ed = ec * cos(rlat_a)

    dx = (rlon_b - rlon_a) * ec
    dy = (rlat_b - rlat_a) * ed
    if dy == 0:
        angle = 90.
    else:
        angle = atan(abs(dx / dy)) * 180.0 / pi
    dlon = lon_b - lon_a
    dlat = lat_b - lat_a
    if dlon > 0 and dlat <= 0:
        angle = (90. - angle) + 90
    elif dlon <= 0 and dlat < 0:
        angle = angle + 180
    elif dlon < 0 and dlat >= 0:
        angle = (90. - angle) + 270
    return angle


def compute_error(true_pt, pred_pt):
    lat1 = float(true_pt[1])
    lng1 = float(true_pt[0])
    lat2 = float(pred_pt[1])
    lng2 = float(pred_pt[0])
    radLat1 = rad(lat1)
    radLat2 = rad(lat2)
    a = radLat1 - radLat2
    b = rad(lng1) - rad(lng2)
    s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)))
    s = s * 6378.137
    s = round(s * 10000) / 10
    return s


def get_id(x, delimiter):
    x = x.strip().split(delimiter)
    return [int(x[-2]), int(x[-1])]


def make_dataset(df, gongcan):
    df = df[df['All-LAC'].notnull() & df['All-Cell Id'].notnull() & df['All-Longitude'].notnull() & df['All-Latitude'].notnull() & (
                df['Message Type'] == 'Measurement Report')]
    # enodebid = np.array(map(lambda x: get_id(x, '/'), df['ECI(eNodeBID/CellID)'].values), dtype=np.int)
    # df.loc[:, 'eNodeBID'] = enodebid[:, 0]
    # df.loc[:, 'CellID'] = enodebid[:, 1]
    df = df.merge(gongcan, left_on=['All-LAC', 'All-Cell Id'], right_on=['LAC', 'CI'], how='left')
    for i in range(1, 7):
        df = df.merge(gongcan, left_on=['All-Neighbor LAC (Sorted)[%d]' % i, 'All-Neighbor Cell Id (Sorted)[%d]' % i],
                      right_on=['LAC', 'CI'], how='left', suffixes=('', '_%d' % i))
    df = df.fillna(-999)
    label = df.loc[:, ['All-Longitude', 'All-Latitude']].values
    t = df.loc[:, 'Time'].values

    def compute_second(x):
        x = x.strip().split(':')
        return int(x[0]) * 3600 + int(x[1]) * 60 + int(x[2])

    t = np.array(map(compute_second, t), dtype=np.int32)
    df = df.drop(['All-Longitude', 'All-Latitude', 'Time', 'MS', 'Frame Number', 'Direction', 'Message Type', 'Event', 'EventInfo'], axis=1)
    bst = df.loc[:, ['Longitude', 'Latitude']].values
    dist = []
    for bst_pt, true_pt in zip(bst, label):
        if bst_pt[0] > 0:
            dist.append(compute_error(bst_pt, true_pt))
    print 'Mean Distance\t%f' % np.mean(dist)
    print 'Median Distance\t%f' % np.median(dist)
    print 'Max Distance\t%f' % np.max(dist)
    return t, label, df.values, df.columns


def prepare_gongcan(df):
    enodebid = np.array(map(lambda x: get_id(x, '-'), df['CGI'].values), dtype=np.int)
    df.loc[:, 'eNodeBID'] = enodebid[:, 0]
    df.loc[:, 'CellID'] = enodebid[:, 1]
    df = df.drop(['ENBid', 'CGI'], axis=1)
    return df


def feature_engineer(feature, pred, timestamp):
    add_feature = []

    for i in range(0, len(pred)):
        if i == 0:
            last_pt = pred[i]
            last_time = timestamp[i]
        else:
            last_pt = pred[i - 1]
            last_time = timestamp[i - 1]
        if i == len(pred) - 1:
            next_pt = pred[i]
            next_time = timestamp[i]
        else:
            next_pt = pred[i + 1]
            next_time = timestamp[i + 1]
        sub_add_feature = []
        sub_add_feature.append(compute_error(last_pt, pred[i]))
        sub_add_feature.append(compute_error(next_pt, pred[i]))
        sub_add_feature.append(azimuth(last_pt, pred[i]))
        sub_add_feature.append(azimuth(pred[i], next_pt))
        sub_add_feature.append(timestamp[i] - last_time if timestamp[i] - last_time > 0 else 0)
        sub_add_feature.append(next_time - timestamp[i] if next_time - timestamp[i] > 0 else 0)
        sub_add_feature.append(sub_add_feature[0] / sub_add_feature[4] if sub_add_feature[4] > 0 else 0)
        sub_add_feature.append(sub_add_feature[1] / sub_add_feature[5] if sub_add_feature[5] > 0 else 0)
        sub_add_feature.append(last_pt[0])
        sub_add_feature.append(last_pt[1])
        sub_add_feature.append(next_pt[0])
        sub_add_feature.append(next_pt[1])
        sub_add_feature.append(pred[i][0])
        sub_add_feature.append(pred[i][1])
        add_feature.append(sub_add_feature)
    feature = np.hstack((feature, add_feature))
    return feature


if __name__ == '__main__':
    gongcan = pd.read_csv('2g_gongcan.csv')
    # gongcan = prepare_gongcan(gongcanDF)

    trainDF = pd.read_csv('forward_recovered.csv')
    # trainDF = pd.read_csv('xietu_train.csv')
    tr_time, tr_label, tr_feature, tr_feature_name = make_dataset(trainDF, gongcan)

    testDF = pd.read_csv('backward.csv')
    # testDF = pd.read_csv('xietu_test_recovered.csv')
    te_time, te_label, te_feature, te_feature_name = make_dataset(testDF, gongcan)

    # first layer
    print 'Train Layer 1 ...'
    est_1 = RandomForestRegressor(
        n_jobs=-1,
        # max_depth=5,
        n_estimators=1000,
        max_features=0.8,
        bootstrap=False,
        criterion='mse').fit(tr_feature, tr_label)
    # est_1 = DecisionTreeRegressor().fit(tr_feature, tr_label)
    layer1_size = tr_feature.shape[1]

    tr_pred = est_1.predict(tr_feature)
    tr_feature = feature_engineer(tr_feature, tr_pred, tr_time)

    te_pred = est_1.predict(te_feature)
    te_feature = feature_engineer(te_feature, te_pred, te_time)

    print 'Train Layer 2 ...'
    est_2 = RandomForestRegressor(
        n_jobs=-1,
        # max_depth=5,
        n_estimators=600,
        max_features='sqrt',
        bootstrap=False,
        criterion='mse').fit(tr_feature, tr_label)
    # est_2 = DecisionTreeRegressor().fit(tr_feature, tr_label)
    layer2_size = tr_feature.shape[1]

    tr_feature_name = list(tr_feature_name)
    for item in ['p_dist', 'n_dist', 'p_angle', 'n_angle', 'p_timegap', 'n_timegap', 'p_speed', 'n_speed', 'p_lon', 'p_lat', 'n_lon',
                 'n_lat', 'pred_lon', 'pred_lat']:
        tr_feature_name.append(item)

    importance = est_2.feature_importances_
    idx = np.argsort(-importance)
    f_imp = open('importance.txt', 'a')
    ii = 1
    for i in idx:
        f_imp.write('%d\t%s\t%f\n' % (ii, tr_feature_name[i], importance[i]))
        ii += 1
    f_imp.write('\n')

    te_pred = est_2.predict(te_feature)

    print 'Generate Report ...'
    tot_error = []
    f = open('traj.txt', 'w')
    # f.write('11111')
    for t, pred_pt, true_pt in zip(te_time, te_pred, te_label):
        # f.write(',%d,%.6f,%.6f,%.6f,%.6f' % (t, true_pt[0], true_pt[1], pred_pt[0], pred_pt[1]))
        f.write('%.6f,%.6f\n' % (pred_pt[0], pred_pt[1]))
        tot_error.append(compute_error(pred_pt, true_pt))
    # f.write('\n')
    f_report = open('report.txt', 'a')
    f_report.write('Total Train size\t%d\n' % tr_feature.shape[0])
    f_report.write('Total Test size\t%d\n' % len(tot_error))
    # f_report.write('Feature Size\t%d\t%d\n' % (layer1_size, layer2_size))
    tot_error = sorted(tot_error)
    f_report.write('Total Max error\t%f\n' % np.max(tot_error))
    f_report.write('Total Min error\t%f\n' % np.min(tot_error))
    f_report.write('Total Mean error\t%f\n' % np.mean(tot_error))
    f_report.write('Total Median error\t%f\n' % np.median(tot_error))
    f_report.write('Total 67%% error\t%f\n\n' % tot_error[int(len(tot_error) * 0.67)])
