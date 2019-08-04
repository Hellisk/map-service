import logging
import os

import matplotlib
import networkx as nx
import pandas as pd
import time

matplotlib.use('Agg')
from matplotlib import pyplot as plt
# %matplotlib inline
from sacred import Experiment
from sacred.observers import MongoObserver
import trajmap
import gis
from PIL import Image
import StringIO


def init():
    log_id = str(int(time.time() * 10) % (60 * 60 * 24 * 365 * 10)) + str(os.getpid())
    global logger
    logger = logging.getLogger(str(log_id))

    logger.setLevel(logging.DEBUG)

    # write to file 
    fh = logging.FileHandler('ex.log')
    fh.setLevel(logging.DEBUG)

    # write to console
    ch = logging.StreamHandler()
    ch.setLevel(logging.DEBUG)

    # Handler format
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s -\n\t%(message)s')
    fh.setFormatter(formatter)
    ch.setFormatter(formatter)

    logger.addHandler(fh)
    logger.addHandler(ch)

    global ex
    ex = Experiment('DNLP')
    ex.logger = logger
    ex.observers.append(MongoObserver.create(url='localhost:27017', db_name='DNLP'))
    # ex.observers.append(MongoObserver.create(url='127.0.0.1:27017', db_name='nTrajMap'))
    return ex, logger


def show_png(fid, db, fig_width=10):
    png = db['default.chunks'].find_one({'files_id': fid})['data']
    im = Image.open(StringIO.StringIO(png))
    w, h = im.size
    plt.figure(figsize=(fig_width, fig_width * h / w))
    plt.imshow(im)
    plt.axis('off')
    plt.show()


def file_names(arts, db):
    names = []
    for art in arts:
        names.append(db['default.files'].find_one({'_id': art})['filename'])
    return names


def remove_duplicate(recs):
    config_set = set()
    exs = []
    for ex in sorted(recs, key=lambda x: x['_id'])[::-1]:
        cfg = ex['config']
        cfg['seed'] = 0
        # cfg['ex_name'] = 0
        # print cfg
        if str(cfg) in config_set:
            continue
        exs.append(ex)
        config_set.add(str(cfg))
    return exs


init()


@ex.config
def cfg():
    data = 'trajmap_k'
    side = 20
    k = 100
    ratio = None
    percent = 0.02
    width = 4
    alpha = None
    beta = 0
    fig_width = 5
    max_value = 0.2
    combine_dist = 1.5
    topic_model = 'pLSA'
    cands_num = None
    ex_name = data
    sel_cand_method = None
    maxiter = 30


@ex.automain
def main(ex_name, data_file, map_min_x, map_max_x, map_min_y, map_max_y, side, k, ratio, topic_model, percent, width, alpha,
         sel_cand_method, maxiter, cands_num, fig_width, max_value, output_folder, combine_dist, _log, _run, beta, inc=0,
         true_pass=False, cut_length=0):
    # if data == 'chicago':
    #     data_file = "../Data/Chicago/chicago.pickle"
    #     axis = gis.chc_utm_axis
    #     map_file = "../Data/Chicago/min_map_df.csv"
    # elif data == 'minsh_2000':
    #     data_file = "../Data/Shanghai/minsh_2000.pickle"
    #     axis = gis.minsh_utm_axis
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'minsh_4000':
    #     data_file = "../Data/Shanghai/minsh_4000.pickle"
    #     axis = gis.minsh_utm_axis
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'minsh_6000':
    #     data_file = "../Data/Shanghai/minsh_6000.pickle"
    #     axis = gis.minsh_utm_axis
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'minsh_8000':
    #     data_file = "../Data/Shanghai/minsh_8000.pickle"
    #     axis = gis.minsh_utm_axis
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'minsh_10000':
    #     data_file = "../Data/Shanghai/minsh_10000.pickle"
    #     axis = gis.minsh_utm_axis
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'minsh_sparse_1000':
    #     data_file = "../Data/Shanghai/minsh_sparse_1000.pickle"
    #     axis = gis.minsh_utm_axis
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'minsh_sparse_5000':
    #     data_file = "../Data/Shanghai/minsh_sparse_5000.pickle"
    #     axis = gis.minsh_utm_axis
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'minsh_sparse_10000':
    #     data_file = "../Data/Shanghai/minsh_sparse_10000.pickle"
    #     axis = gis.minsh_utm_axis
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'maxsh_20000':
    #     data_file = "../Data/Shanghai/maxsh_20000.pickle"
    #     axis = gis.maxsh_utm_axis
    #     # map_file = "../Data/Shanghai/sh_map_df.csv"
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'maxsh_40000':
    #     data_file = "../Data/Shanghai/maxsh_40000.pickle"
    #     axis = gis.maxsh_utm_axis
    #     # map_file = "../Data/Shanghai/sh_map_df.csv"
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'maxsh_60000':
    #     data_file = "../Data/Shanghai/maxsh_60000.pickle"
    #     axis = gis.maxsh_utm_axis
    #     # map_file = "../Data/Shanghai/sh_map_df.csv"
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'maxsh_80000':
    #     data_file = "../Data/Shanghai/maxsh_80000.pickle"
    #     axis = gis.maxsh_utm_axis
    #     # map_file = "../Data/Shanghai/sh_map_df.csv"
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    # elif data == 'maxsh_100000':
    #     data_file = "../Data/Shanghai/maxsh_100000.pickle"
    #     axis = gis.maxsh_utm_axis
    #     # map_file = "../Data/Shanghai/sh_map_df.csv"
    #     map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    axis = (map_min_x, map_max_x, map_min_y, map_max_y)

    global ex
    # ex.add_artifact('origin_gen_map.png')
    # return {}
    # map_df = pd.read_csv(map_file)
    # map_df = gis.scale_map_by_axis(map_df, axis)
    # map_df.index = map_df.rid

    _run.info['ex_name'] = ex_name
    print (topic_model, data_file, side, k, percent, width, alpha, beta)
    _log.info('topic_model: %s \tdata_file: %s \tside: %d\tk: %d\tpercent: %.4f\twidth: %d\talpha: %.2f\tbeta: %.2f' % (
        topic_model, data_file, side, k, percent, width, alpha, beta))

    trajs = pd.read_csv(data_file, sep=',', names=['id', 'x', 'y', 't', 'tid'])
    result = {}

    _run.info['time'] = {}
    _run.info['side_length'] = gis.distance(axis[0], axis[2], axis[0], axis[2] + side, utm=True)

    total_time = 0
    t0 = time.time()
    mat = trajmap.Sag(trajs, side, inc=inc, cut_length=cut_length)
    t1 = time.time()
    total_time += t1 - t0
    _run.info['time']['1_Make_matrix'] = t1 - t0
    _run.info['matrix_shape'] = (mat.width, mat.height)
    delta_time = None

    io_time = 0
    t0 = time.time()
    if topic_model == 'pLSA':
        trajmap.get_P(mat)
        mat.G = nx.DiGraph()
        mat.candidates = set()
        mat.pLSA(k, maxiter)
        trajmap.iteration(mat, width, k, percent, max_value, true_pass)
    elif topic_model == 'LDA':
        trajmap.get_P(mat)
        mat.G = nx.DiGraph()
        mat.candidates = set()
        io_time = mat.LDA(k, maxiter, alpha, beta)
        trajmap.iteration(mat, width, k, percent, max_value, true_pass)
    elif topic_model == 'pLSA_cut':
        trajmap.get_P(mat)
        # mat.sag = mat.sag.toarray()
        trajmap.generate_map(mat, width, k, maxiter, percent, max_value)
    t1 = time.time()

    total_time += t1 - t0 - io_time
    _run.info['time']['Generate_Map'] = t1 - t0
    _run.info['candidates_num'] = len(mat.candidates)
    _run.info['candidates'] = list(mat.candidates)
    '''
    result['candidates_error'] = gis.candidates_error(mat, map_df)

    plot.map_df(map_df, ca, axis=axis)
    plt.axis(axis)
    plt.savefig('candidates.png')
    ex.add_artifact('candidates.png')
    '''

    _M = mat.G.copy()
    '''
    t0 = time.time()
    gps = {}
    M, gps = trajmap.smooth_M(mat, mat.G, gps, _dist=combine_dist)
    t1 = time.time()
    total_time += t1-t0
    _run.info['time']['4_Smooth_map'] = t1-t0
    '''
    _run.info['total_time'] = total_time

    gen_map_df = gis.convert_nx2map_df(mat, _M, {})

    result['origin'] = {}
    # if axis[0] == gis.maxsh_utm_axis[0]:
    #     result['origin'] = gis.evaluate_map(gen_map_df, map_df, gis.minsh_utm_axis)
    # else:
    #     result['origin'] = gis.evaluate_map(gen_map_df, map_df, axis)
    result['origin']['length'] = gis.map_length(gen_map_df)
    result['time'] = total_time

    # plt.figure(figsize=(fig_width, fig_width * (axis[3] - axis[2]) / (axis[1] - axis[0])))
    # ca = plt.gca()
    # plot.map_df(map_df, ca=ca, axis=axis, color='b')
    # plot.map_df(gen_map_df, ca=ca, axis=axis, color='r', linewidth=1.5)
    # plot.dots(mat, mat.candidates, ca=ca, gps=True, color='r')
    # plt.axis(axis)
    # plt.savefig('origin_gen_map.png')
    #
    # ex.add_artifact('origin_gen_map.png')

    gen_map_name = output_folder + "inferred_map_CRIF.txt"
    # csv_name = '../Data/csv/%s_%s_%d_%d_%d_%f_%d_%f.csv' % (data, topic_model, side, k, width, max_value, maxiter, percent)
    gen_map_df.to_csv(gen_map_name)
    ex.add_artifact(gen_map_name)

    M = mat.G.copy()
    # if data == 'minsh_6000' and ex_name == 'visualization':
    # if ex_name == 'visualization':
    #     result['smoothed'] = {}
    #     for _dist in [1.5, 2, 3]:
    #         _M, gps = trajmap.smooth_M(mat, M.to_directed(), {}, _dist)
    #         gen_map_df = gis.convert_nx2map_df(mat, _M, gps)
    #         result['smoothed'][_dist] = gis.evaluate_map(gen_map_df, map_df, axis)
    #         # result['smoothed']['length'] = gis.map_length(gen_map_df)
    #
    #         plt.figure(figsize=(fig_width, fig_width * (axis[3] - axis[2]) / (axis[1] - axis[0])))
    #         ca = plt.gca()
    #         plot.map_df(map_df, ca=ca, axis=axis, color='b')
    #         plot.map_df(gen_map_df, ca=ca, axis=axis, color='r', linewidth=2)
    #         plt.axis(axis)
    #         plt.savefig('gen_map.png')
    #         ex.add_artifact('gen_map.png')

    return result
