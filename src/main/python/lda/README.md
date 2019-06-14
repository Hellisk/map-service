# Topic Model-based Road Network Inference from Massive Trajectories

This project is the implementation of the paper [Topic Model-based Road Network Inference from Massive Trajectories](http://www.renj.me/roadnet.pdf) (MDM 2017)

## 1. Data

Data Structure: (tid, x, y, lon, lat, time)

### 1.1 Chicago

* Trajectories
    - utm_axis = (442551, 447326,  4634347, 4637377)
    - gps_axis = (41.858952 -87.69215 41.886565 -87.634896)
    - TrajMap
        + /Data/Chicago/chicago.pickle
            + id, x (utm), y (utm), t, tid
    - Biagioni
        + /Data/Chicago/all_trips
* Real map
    - osm
        + /Data/Chicago/chicago_edges_osm.txt
        + /Data/Chicago/chicago_vertices_osm.txt
    - dataframe
        + /Data/Chicago/chicago_map_df.csv

### 1.2 Shanghai

* Trajectories
    * Shanghai small data
        - utm_axis = (347500, 352500, 3447500, 3452500)
        - gps_axis = (121.4, 121.452, 31.1515, 31.197) 
        - Data:
            + /Data/Shanghai/minsh_1000.pickle
            + /Data/Shanghai/minsh_1000_biagioni
            + /Data/Shanghai/minsh_5000.pickle
            + /Data/Shanghai/minsh_10000.pickle
    * Shanghai big data:
        - utm_axis = (345000, 365000, 3445000, 3465000)
* Map:
    - /Data/Shanghai/sh_map_df.csv

### 1.3 Biagioni

chicago_biagioni LineSegment 798 3.7801833333333335

* data need to change
    - ./bounding_boxes
* data need store
    - ./skeleton_maps/*
* data after runing
    - ./*.png
    - ./trips/*
    - ./skeleton_images/*
    - ./skeleton_maps/*

## 2. Run experiment script

```bash
python script.py
```

## 3. Map-matching

```python
data.index = range(len(data))
data[['tLon','tLat','pLon','pLat']]
```

## 4. compile plsa

```bash
python setup.py build_ext --inplace --force
```

## 5. Parameters

* $u_{ij}$: topic matrix with $j$ th topic and $i$ th cell
* $k$: number of topics
* $w$: grid width
* $p$: padding