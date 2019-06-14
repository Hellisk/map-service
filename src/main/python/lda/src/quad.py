import math
import numpy as np
import sys
import time
from pympler import asizeof

# import CBitlist
import tools

g_max_depth = 1024
g_max_nodes = 20


class TreeList(list):
    def is_leaf(self):
        if len(self) == 2 and type(self[0]) is int and type(self[1]) is int:
            return True
        else:
            return False

    def leaf_number(self):
        if self.is_leaf():
            return 1
        else:
            return sum([child.leaf_number() for child in self])

    def is_covered(self, _x0, _x1, _y0, _y1, x0, x1, y0, y1):
        """
        if (x0, x1, y0, y1) intersect (_x0, _x1, _y0, _y)
        """
        return not ((x0 > _x1) or (x1 < _x0) or (y1 < _y0) or (y0 > _y1))

    def is_totaly_covered(self, _x0, _x1, _y0, _y1, x0, x1, y0, y1):
        return ((x0 <= _x0) and (x1 >= _x1) and (y0 <= _y0) and (y1 >= _y1))

    def meet_trajectories_detail_list(self, x0, x1, y0, y1, _x0=-180.0, _x1=180.0, _y0=-90.0, _y1=90.0):
        if self.is_covered(_x0, _x1, _y0, _y1, x0, x1, y0, y1):
            if self.is_leaf():
                if self[0] is -1:
                    return None
                else:
                    return [(self[0], self.is_totaly_covered(_x0, _x1, _y0, _y1, x0, x1, y0, y1))]
            else:
                ret = []
                x_mid = (_x0 + _x1) / 2
                y_mid = (_y0 + _y1) / 2
                reg = [(_x0, x_mid, _y0, y_mid),
                       (_x0, x_mid, y_mid, _y1),
                       (x_mid, _x1, _y0, y_mid),
                       (x_mid, _x1, y_mid, _y1)]
                for i in range(len(self)):
                    p0, p1, p2, p3 = reg[i][0], reg[i][1], reg[i][2], reg[i][3]
                    _ret = self[i].meet_trajectories_detail_list(x0, x1, y0, y1, p0, p1, p2, p3)
                    if _ret is not None:
                        for _r in _ret:
                            ret.append(_r)
                return ret

    def meet_trajectories_detail_set(self, x0, x1, y0, y1, _x0=-180.0, _x1=180.0, _y0=-90.0, _y1=90.0):
        node_list = self.meet_trajectories_detail_list(x0, x1, y0, y1, _x0, _x1, _y0, _y1)
        total_set = set([l[0] for l in node_list if l[1] is True])
        part_set = set([l[0] for l in node_list if l[1] is False]) - total_set
        return total_set, part_set

    def get_size(self):
        if self.is_leaf():
            return sys.getsizeof(self) * 8
        else:
            return sum([child.get_size() for child in self])

    def region_size(self, cid, _x0=-180.0, _x1=180.0, _y0=-90.0, _y1=90.0, depth=0):
        if self.is_leaf():
            if depth == cid:
                return list([tools.haversine(_x0, _y0, _x1, _y1)])
        else:
            x_mid = (_x0 + _x1) / 2
            y_mid = (_y0 + _y1) / 2
            reg = [(_x0, x_mid, _y0, y_mid), (_x0, x_mid, y_mid, _y1), (x_mid, _x1, _y0, y_mid), (x_mid, _x1, y_mid, _y1)]
            ret = []
            for i in range(len(self)):
                p0, p1, p2, p3 = reg[i][0], reg[i][1], reg[i][2], reg[i][3]
                _ret = self[i].region_size(cid, p0, p1, p2, p3, depth + 1)
                if _ret is not None:
                    ret += _ret
            return ret

    def get_cid_depth(self, depth=0):
        if self.is_leaf():
            if self[0] is -1:
                return None
            else:
                return [(self[0], depth)]
        else:
            tids = []
            for child in self:
                ret = child.get_cid_depth(depth + 1)
                if ret is not None:
                    for r in ret:
                        tids.append(r)
            return tids

    def get_depth_with_gps(self, _x0=-180.0, _x1=180.0, _y0=-90.0, _y1=90.0, depth=0):
        if self.is_leaf():
            if self[0] is -1:
                return None
            else:
                return [(self[0], depth, _x0, _x1, _y0, _y1)]
        else:
            x_mid = (_x0 + _x1) / 2
            y_mid = (_y0 + _y1) / 2
            reg = [(_x0, x_mid, _y0, y_mid), (_x0, x_mid, y_mid, _y1), (x_mid, _x1, _y0, y_mid), (x_mid, _x1, y_mid, _y1)]
            ret = []
            for i in range(len(self)):
                p0, p1, p2, p3 = reg[i][0], reg[i][1], reg[i][2], reg[i][3]
                _ret = self[i].get_depth_with_gps(p0, p1, p2, p3, depth + 1)
                if _ret is not None:
                    ret += _ret
            return ret

    def cover_page_num(self, x0, x1, y0, y1, page_size, _x0=-180.0, _x1=180.0, _y0=-90.0, _y1=90.0):
        if self.is_covered(x0, x1, y0, y1, _x0, _x1, _y0, _y1):
            if self.is_leaf():
                return np.ceil(1.0 * self[1] / page_size)
            else:
                ret = []
                x_mid = (_x0 + _x1) / 2
                y_mid = (_y0 + _y1) / 2
                reg = [(_x0, x_mid, _y0, y_mid),
                       (_x0, x_mid, y_mid, _y1),
                       (x_mid, _x1, _y0, y_mid),
                       (x_mid, _x1, y_mid, _y1)]
                for i in range(len(self)):
                    p0, p1, p2, p3 = reg[i][0], reg[i][1], reg[i][2], reg[i][3]
                    _ret = self[i].cover_page_num(x0, x1, y0, y1, page_size, p0, p1, p2, p3)
                    ret.append(_ret)
                return sum(ret)
        return 0

    def cover_cell_depth(self, x0, x1, y0, y1, _x0=-180.0, _x1=180.0, _y0=-90.0, _y1=90.0, depth=0):
        if self.is_covered(_x0, _x1, _y0, _y1, x0, x1, y0, y1):
            if self.is_leaf():
                if self[0] is -1:
                    return None
                else:
                    return [(self[0], depth, _x0, _x1, _y0, _y1)]
            else:
                ret = []
                x_mid = (_x0 + _x1) / 2
                y_mid = (_y0 + _y1) / 2
                reg = [(_x0, x_mid, _y0, y_mid),
                       (_x0, x_mid, y_mid, _y1),
                       (x_mid, _x1, _y0, y_mid),
                       (x_mid, _x1, y_mid, _y1)]
                for i in range(len(self)):
                    p0, p1, p2, p3 = reg[i][0], reg[i][1], reg[i][2], reg[i][3]
                    _ret = self[i].cover_cell_depth(x0, x1, y0, y1, p0, p1, p2, p3, depth + 1)
                    if _ret is not None:
                        for _r in _ret:
                            ret.append(_r)
                return ret


class QuadTree:

    def __init__(self, depth=0,
                 x0=-180.0, x1=180.0, y0=-90.0, y1=90.0, ):
        """
        x0, y0 is the left bottom point
        x1, y1 is the right up point
        x0 < x1
        y0 < y1
        """
        self.nodes = {}
        self.nodes_keys = set()
        self.children = []
        self.depth = depth
        self.x0 = x0
        self.x1 = x1
        self.y0 = y0
        self.y1 = y1
        self.nodes_num = 0

    def insert(self, tid, x, y):
        if self.is_leaf():
            if tid not in self.nodes_keys:
                if len(self.nodes_keys) >= g_max_nodes:
                    if self.depth == g_max_depth:
                        return
                        # print 'Insert Failure: depth overload'
                    else:
                        self._split()
                        self._insert_into_children(tid, x, y)
                    return
                else:
                    self.nodes[tid] = []
                    self.nodes_keys.add(tid)
            self.nodes[tid].append((x, y))
        else:
            self._insert_into_children(tid, x, y)

    def _split(self):
        x_mid = (self.x0 + self.x1) / 2
        y_mid = (self.y0 + self.y1) / 2
        self.children = [
            QuadTree(self.depth + 1, x0=self.x0, x1=x_mid, y0=self.y0, y1=y_mid),
            QuadTree(self.depth + 1, x0=self.x0, x1=x_mid, y0=y_mid, y1=self.y1),
            QuadTree(self.depth + 1, x0=x_mid, x1=self.x1, y0=self.y0, y1=y_mid),
            QuadTree(self.depth + 1, x0=x_mid, x1=self.x1, y0=y_mid, y1=self.y1),
        ]
        for tid in self.nodes_keys:
            for x, y in self.nodes[tid]:
                self._insert_into_children(tid, x, y)
        self.nodes = {}

    def _insert_into_children(self, tid, x, y):
        for child in self.children:
            if child.is_contain(x, y):
                child.insert(tid, x, y)
                break

    def set_nodes_num(self):
        if self.is_leaf():
            self.nodes_num = len(self.nodes)
        else:
            self.nodes_num = sum([child.set_nodes_num() for child in self.children])
        return self.nodes_num

    def set_min_nodes(self, min_nodes):
        pass

    def display(self):
        # if len(self.nodes) != 0: print len(self.nodes)
        if len(self.children) == 0:
            if len(self.nodes) == 0:
                return
            print '*' * self.depth, self.id, len(self.nodes)
        else:
            for child in self.children:
                child.display()

    def generate_list(self):
        if self.is_leaf():
            return [self.nodes.keys()]
        else:
            ret = []
            for child in self.children:
                ret += child.generate_list()
            return ret

    def is_leaf(self):
        if len(self.children) == 0:
            return True
        return False

    def leaf_number(self):
        if self.is_leaf():
            return 1
        else:
            return sum([child.leaf_number() for child in self.children])

    def generate_leaf_list(self):
        if self.is_leaf():
            if len(self.nodes) is 0:
                return []
            else:
                return [self.nodes.keys()]
        else:
            ret = []
            for child in self.children:
                _ret = child.generate_leaf_list()
                if _ret != []:
                    ret += _ret
            return ret

    def is_covered(self, x0, x1, y0, y1):
        return not ((x0 > self.x1) or (x1 < self.x0) or (y1 < self.y0) or (y0 > self.y1))

    def is_totaly_covered(self, x0, x1, y0, y1):
        return ((x0 <= self.x0) and (x1 >= self.x1) and (y0 <= self.y0) and (y1 >= self.y1))

    def is_contain(self, x, y):
        return ((x > self.x0) and (x < self.x1) and (y > self.y0) and (y < self.y1))

    def cover_leaf_num(self, x0, x1, y0, y1):
        if self.is_covered(x0, x1, y0, y1):
            if self.is_leaf():
                return 1
            else:
                return sum([child.cover_leaf_num(x0, x1, y0, y1) for child in self.children])
        return 0

    def cover_page_num(self, x0, x1, y0, y1, page_size):
        if self.is_covered(x0, x1, y0, y1):
            if self.is_leaf():
                return int(math.ceil(1.0 * sum([len(self.nodes[k]) for k in self.nodes.keys()]) / page_size))
            else:
                return sum([child.cover_page_num(x0, x1, y0, y1, page_size) for child in self.children])
        return 0

    def get_tid(self):
        if self.is_leaf():
            return self.nodes.keys()
        else:
            tids = []
            for child in self.children:
                tids += child.get_tid()
            return tids

    def get_tid_depth(self, depth=0):
        if self.is_leaf():
            return zip(self.nodes.keys(), [depth] * len(self.nodes))
        else:
            tids = []
            for child in self.children:
                tids += child.get_tid_depth(depth + 1)
            return tids

    """
    def cover_leaf_list(self, x0, x1, y0, y1):
        if self.is_covered(x0, x1, y0, y1):
            if self.is_leaf():
                return self.nodes.keys()
            else:
                ret = []
                for child in self.children:
                    _ret = child.cover_leaf_list(x0, x1, y0, y1)
                    if _ret != None:
                        ret += _ret
                return ret
    """

    def meet_trajectories_detail_list(self, x0, x1, y0, y1):
        """
        Check how many trajectories are realy covered
        """
        if self.is_covered(x0, x1, y0, y1):
            if self.is_leaf():
                return zip(self.nodes.keys(), [self.is_totaly_covered(x0, x1, y0, y1)] * len(self.nodes))
            else:
                ret = []
                for child in self.children:
                    _ret = child.meet_trajectories_detail_list(x0, x1, y0, y1)
                    if _ret is not None:
                        ret += _ret
                return ret

    def meet_trajectories_detail_set(self, x0, x1, y0, y1):
        node_list = self.meet_trajectories_detail_list(x0, x1, y0, y1)
        total_set = set([l[0] for l in node_list if l[1] is True])
        part_set = set([l[0] for l in node_list if l[1] is False]) - total_set
        return total_set, part_set

    def meet_trajectories_set(self, x0, x1, y0, y1):
        total, part = self.meet_trajectories_detail_set(x0, x1, y0, y1)
        return total | part

    """
    def cover_trajectories_detail_list(self, x0, x1, y0, y1):
        '''
        Check how many trajectories are realy covered
        '''
        if self.is_covered(x0, x1, y0, y1):
            if self.is_leaf():
                ret = []
                flag = False
                for t in self.nodes.keys():
                    for (x, y) in self.nodes[t]:
                        #print x,y
                        if ((x > x0) and (x < x1) and (y > y0) and (y < y1)):
                            flag = True
                            break
                    ret.append((t, flag))
                return ret
            else:
                ret = []
                for child in self.children:
                    _ret = child.cover_trajectories_detail_list(x0, x1, y0, y1)
                    if _ret is not None:
                        ret += _ret
                return ret

    def cover_trajectories_detail_set(self, x0, x1, y0, y1):
        node_list = self.cover_trajectories_detail_list(x0, x1, y0, y1)
        pos_set = set([l[0] for l in node_list if l[1] is True])
        neg_set = set([l[0] for l in node_list if l[1] is False])-pos_set
        return pos_set, neg_set

    def cover_trajectories_set(self, x0, x1, y0, y1):
        pos, neg = self.cover_trajectories_detail_set(x0, x1, y0, y1)
        return pos | neg
    """

    """
    def _change_cid(self):
        #return self.max_id
        return self.id
    """

    def remove_points(self):
        if self.is_leaf() is True:
            for k in self.nodes.keys():
                self.nodes[k] = []
        else:
            for child in self.children:
                child.remove_points()

    def get_size(self):
        return asizeof.asizeof(self)

    def tree_list_rec(self, cid=-1):
        ret = TreeList()
        if self.is_leaf():
            if len(self.nodes) is not 0:
                cid += 1
            else:
                cid = -1
        else:
            for child in self.children:
                _ret, _cid = child.tree_list_rec(cid)
                if child.is_leaf():
                    s = sum([len(child.nodes[k]) for k in child.nodes.keys()])
                    ret.append(TreeList([_cid, s]))
                else:
                    ret.append(_ret)
                if _cid is not -1:
                    cid = _cid
        return ret, cid

    def tree_list(self):
        return self.tree_list_rec()[0]


class GridTree(QuadTree):

    def __init__(self, max_depth, depth=0,
                 x0=-180.0, x1=180.0, y0=-90.0, y1=90.0, ):
        """
        x0, y0 is the left bottom point
        x1, y1 is the right up point
        x0 < x1
        y0 < y1
        """
        self.nodes = {}
        self.children = []
        self.depth = depth
        self.max_depth = max_depth
        self.x0 = x0
        self.x1 = x1
        self.y0 = y0
        self.y1 = y1

    def build(self, d):
        d.index = range(len(d))
        t1 = time.time()
        for i in range(len(d)):
            x = d.ix[i]
            self.insert(x.tid, x.x, x.y)
            if i % int((len(d)) / 10) == 0:
                print 'Proccessed %d from %d' % (i, len(d))
        t2 = time.time()
        print 'Time: ', t2 - t1
        return t2 - t1

    def insert(self, tid, x, y):
        if self.is_leaf():
            if tid not in self.nodes.keys():
                self.nodes[tid] = []
            self.nodes[tid].append((x, y))
        else:
            if self.children == []:
                self._split()
            self._insert_into_children(tid, x, y)

    def _split(self):
        x_mid = (self.x0 + self.x1) / 2
        y_mid = (self.y0 + self.y1) / 2
        self.children = [
            GridTree(self.max_depth, self.depth + 1, x0=self.x0, x1=x_mid, y0=self.y0, y1=y_mid),
            GridTree(self.max_depth, self.depth + 1, x0=self.x0, x1=x_mid, y0=y_mid, y1=self.y1),
            GridTree(self.max_depth, self.depth + 1, x0=x_mid, x1=self.x1, y0=self.y0, y1=y_mid),
            GridTree(self.max_depth, self.depth + 1, x0=x_mid, x1=self.x1, y0=y_mid, y1=self.y1),
        ]
        for tid in self.nodes.keys():
            for x, y in self.nodes[tid]:
                self._insert_into_children(tid, x, y)
        self.nodes = {}

    def _insert_into_children(self, tid, x, y):
        for child in self.children:
            if child.is_contain(x, y):
                child.insert(tid, x, y)
                break

    def is_leaf(self):
        return self.depth == self.max_depth


class Index():
    def __init__(self, tree, blist):
        self.tree = tree
        self.blist = blist

    def meet_trajectories_set(self, x0, x1, y0, y1):
        total, part = self.meet_trajectories_detail_set(x0, x1, y0, y1)
        return total | part

    def meet_trajectories_detail_set(self, x0, x1, y0, y1):
        total_cell, part_cell = self.tree.meet_trajectories_detail_set(x0, x1, y0, y1)
        pos_set = set()
        for t in self.blist.traj_in_cells(total_cell):
            pos_set.add(t)
        part_set = set()
        for t in self.blist.traj_in_cells(part_cell):
            part_set.add(t)
        part_set = part_set - pos_set
        return pos_set, part_set

    def cover_page_num(self, x0, x1, y0, y1, page_size):
        ret = self.tree.cover_page_num(x0, x1, y0, y1, page_size)
        return ret

    def get_tid_depth(self):
        cid_depth = self.tree.get_cid_depth()
        ret = []
        for cid, depth in cid_depth:
            trajs = self.blist.traj_in_cells(cid)
            ret += zip(trajs, [depth] * len(trajs))
        return ret

    def get_tid(self):
        cid_depth = self.tree.get_cid_depth()
        ret = []
        for cid, depth in cid_depth:
            trajs = self.blist.traj_in_cells(cid)
            ret += trajs
        return ret


class Bitlist():
    def __init__(self, M, w, transform=None):
        self.base = w
        self.bitlist = getattr(CBitlist, 'CBitlist_' + str(w))()
        self.bitlist.Init(M.shape[0])
        cids = M.nonzero()[0]
        t_columns = np.floor(M.nonzero()[1] * 1.0 / w) * w
        w_columns = M.nonzero()[1] - np.floor(M.nonzero()[1] * 1.0 / w) * w
        for c, t, w in zip(cids, t_columns, w_columns):
            self.insert(c, t, w)
        self.transform = None
        if transform is not None:
            self.transform = transform
            self.transform_map = {}
            for i in range(len(transform)):
                self.transform_map[i] = transform[i]

    def insert(self, cid, tid, w):
        self.bitlist.Insert(int(cid), int(tid), int(w))

    def traj_in_cells(self, cids):
        if type(cids) is set:
            cids = list(cids)
        if type(cids) is not list:
            cids = [cids]
        traj_set = set()
        for cid in cids:
            row_trajs = self.bitlist.GetRowPy(cid)
            traj_set |= set(row_trajs)
        ret = []
        if self.transform is not None:
            for traj in traj_set:
                ret.append(self.transform_map[traj])
        else:
            ret = list(traj_set)
        return ret
