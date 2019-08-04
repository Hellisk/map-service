# os.chdir('../')
import os

import matplotlib
# os.chdir('../')
import sys
import time

matplotlib.use('Agg')
from matplotlib import pyplot as plt
# %matplotlib inline
from sacred import Experiment
from sacred.observers import MongoObserver

sys.path.append("../src")

# log_id = str(int(time.time()*10)%(60*60*24*365*10))+str(os.getpid())
# logger = logging.getLogger(str(log_id))

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

ex = Experiment('Biagioni')
ex.logger = logger
ex.observers.append(MongoObserver.create(url='localhost:27017', db_name='Biagioni'))


@ex.config
def cfg():
    # data_file ="../../Data/Shanghai/minsh_1000_biagioni"
    data_file = "../Data/Chicago/all_trips"


@ex.automain
def main(data_file, side, k, percent, width, alpha, _log, _run):
    _log.info('data_file: %s' % (data_file))

    _run.info['time'] = {}

    total_time = 0
    t0 = time.time()

    # 1) Create KDE (kde.png) from trips
    os.system('python kde.py -p $path')

    # 2) Create grayscale skeleton (skeleton.png) from KDE
    python
    skeleton.py
    kde.png
    skeleton.png

    # 3) Extract map database (skeleton_maps/skeleton_map_1m.db) from grayscale skeleton
    python
    graph_extract.py
    skeleton.png
    bounding_boxes / bounding_box_1m.txt
    skeleton_maps / skeleton_map_1m.db

    # 4) Map-match trips onto map database
    python
    graphdb_matcher_run.py - d
    skeleton_maps / skeleton_map_1m.db - t $path - o
    trips / matched_trips_1m /

    # 5) Prune map database with map-matched trips, producing pruned map database (skeleton_maps/skeleton_map_1m_mm1.db)
    python
    process_map_matches.py - d
    skeleton_maps / skeleton_map_1m.db - t
    trips / matched_trips_1m / -o
    skeleton_maps / skeleton_map_1m_mm1.db

    # 6) Refine topology of pruned map, producing refined map (skeleton_maps/skeleton_map_1m_mm1_tr.db)
    python
    refine_topology.py - d
    skeleton_maps / skeleton_map_1m_mm1.db - t
    skeleton_maps / skeleton_map_1m_mm1_traces.txt - o
    skeleton_maps / skeleton_map_1m_mm1_tr.db

    # 7) Map-match trips onto refined map
    python
    graphdb_matcher_run.py - d
    skeleton_maps / skeleton_map_1m_mm1_tr.db - t $path - o
    trips / matched_trips_1m_mm1_tr /

    # 8) Prune refined map with map-matched trips, producing pruned refined map database (skeleton_maps/skeleton_map_1m_mm2.db)
    python
    process_map_matches.py - d
    skeleton_maps / skeleton_map_1m_mm1_tr.db - t
    trips / matched_trips_1m_mm1_tr / -o
    skeleton_maps / skeleton_map_1m_mm2.db

    # 9) Output pruned refined map database for visualization (final_map.txt)
    python
    streetmap.py
    graphdb
    skeleton_maps / skeleton_map_1m_mm2.db
    final_map.txt

    rm - r
    kde.png
    skeleton.png
    skeleton_maps / * trips / *


t1 = time.time()
total_time += t1 - t0
_run.info['time'][''] = t1 - t0
_run.info['matrix_shape'] = (mat.width, mat.height)

t0 = time.time()
mat.get_USV(k)
mat.compute_amat()
mat.compute_candidates(show=False, percent=percent, width=width)
t1 = time.time()
total_time += t1 - t0
_run.info['time']['SVD'] = t1 - t0
_run.info['candidates_num'] = len(mat.candidates)

plot.dots(mat, mat.candidates)
plt.savefig('')
ex.add_artifact()

return
t0 = time.time()
mat.pagerank(alpha=alpha, trustrank=True, inverse=True, show=False)
inv_rank = mat.rank
inv_rank_norm = mat.rank_norm
mat.pagerank(alpha=alpha, trustrank=True, inverse=False, show=False)
rank = mat.rank
rank_norm = mat.rank_norm
rank_max = {}
for key in rank_norm.keys():
    rank_max[key] = max(rank_norm[key], inv_rank_norm[key])
t1 = time.time()
total_time += t1 - t0
_run.info['time']['pagerank'] = t1 - t0
