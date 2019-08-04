import logging
import os

import matplotlib
import sys
import time

matplotlib.use('Agg')
# %matplotlib inline
from sacred import Experiment
from sacred.observers import MongoObserver
import trajmap
import gis

sys.path.append('../matcher')
import matcher

reload(matcher)
import pandas as pd
import turn


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
    ex = Experiment('TrajMap')
    ex.logger = logger
    ex.observers.append(MongoObserver.create(url='localhost:27017', db_name='TurnHMM'))
    # ex.observers.append(MongoObserver.create(url='127.0.0.1:27017', db_name='nTrajMap'))
    return ex, logger


init()


@ex.config
def cfg():
    data = 'minsh_2000'
    limit = 100


@ex.automain
def main(ex_name, data, limit, _log, _run):
    if data == 'minsh_1000':
        data_file = "../Data/Shanghai/minsh_1000.pickle"
        axis = gis.minsh_utm_axis
    elif data == 'minsh_2000':
        data_file = "../Data/Shanghai/minsh_2000.pickle"
        axis = gis.minsh_utm_axis
        map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    elif data == 'minsh_4000':
        data_file = "../Data/Shanghai/minsh_4000.pickle"
        axis = gis.minsh_utm_axis
        map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    elif data == 'minsh_6000':
        data_file = "../Data/Shanghai/minsh_6000.pickle"
        axis = gis.minsh_utm_axis
        map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    elif data == 'minsh_8000':
        data_file = "../Data/Shanghai/minsh_8000.pickle"
        axis = gis.minsh_utm_axis
        map_file = "../Data/Shanghai/matched_sh_map_df.csv"
    elif data == 'minsh_10000':
        data_file = "../Data/Shanghai/minsh_10000.pickle"
        axis = gis.minsh_utm_axis
        map_file = "../Data/Shanghai/matched_sh_map_df.csv"

    data_name = data
    data = pd.read_pickle(data_file)
    _gen_map_df = turn.get_gen_map_df()
    roads = turn.get_gen_roads(_gen_map_df)
    side = 15
    mat = trajmap.Sag(roads, side, inc=-1)
    gen_map_df = gis.convert_nx2map_df(mat, mat.tog, {})
    cand_gps, turn_gps, run_time = turn.get_turns_from_data(data, gen_map_df, dist=100, folder='hmm_' + data_name, limit=limit,
                                                            get_time=True)
    _run.info['cand_gps'] = cand_gps
    _run.info['turn_gps'] = turn_gps
    _run.info['time'] = run_time

    result = {}
    result['cand_gps'] = cand_gps
    result['turn_gps'] = turn_gps
    result['time'] = run_time

    return result
