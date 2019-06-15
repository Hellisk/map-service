import os

data = 'minsh_5000'
ex_name = 'new_padding'

side = 10
max_value = 0.1
for width in [10, 11, 12]:
    for cands_num in [1500, 1700, 2000, 2200, 2500]:
        os.system(
            'python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 width=%d alpha=0.9 max_value=%f topic_model=None sel_cand_method=%s cands_num=%d' % (
            ex_name, data, side, 0, width, max_value, 'num_sum', cands_num))

'''
ex, logger = st.init()
ex.logger = logger

st.main(ex_name=data, data=data, side=15, k=250, ratio=1.0, topic_model='SVD', percent=0.35, width=7, alpha=0.9, sel_cand_method=None, cands_num=350, fig_width=10, max_value=0.1, combine_dist=1.5, _run=ex, _log=logger)
# SVD None
side = 10
width = 5
for k in [200, 150, 100]:
    for percent in frange(0.04, 0.015, -0.003):
        for max_value in [0.25]:
        #for max_value in frange(0.1, 0.25, 0.05):
            ret = os.system('python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 percent=%f width=%d alpha=0.9 max_value=%f topic_model=SVD sel_cand_method=None cands_num=None' % (ex_name, data, side, k, percent, width, max_value))
            if ret == 256:
                print 'KeyboardInterrupt'
                exit()
for width in [6,7]:
#for width in [2, 3, 4, 8, 9, 10]:
    #for k in [100, 150, 200, 250]:
    for k in [250, 200, 150, 100]:
        for percent in frange(0.04, 0.015, -0.003):
            for max_value in [0.25]:
            #for max_value in frange(0.1, 0.25, 0.05):
                ret = os.system('python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 percent=%f width=%d alpha=0.9 max_value=%f topic_model=SVD sel_cand_method=None cands_num=None' % (ex_name, data, side, k, percent, width, max_value))
                if ret == 256:
                    print 'KeyboardInterrupt'
                    exit()
# pLSA None
side = 10
width = 6
k = 250
percent = 0.004
max_value = 0.3
ret = os.system('python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 percent=%f width=%d alpha=0.9 max_value=%f topic_model=pLSA sel_cand_method=None cands_num=None' % (ex_name, data, side, k, percent, width, max_value))
for k in [200, 150, 100]:
    for percent in [0.001, 0.002, 0.003, 0.004]:
        #for max_value in frange(0.1, 0.25, 0.05):
        for max_value in [0.3]:
            ret = os.system('python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 percent=%f width=%d alpha=0.9 max_value=%f topic_model=pLSA sel_cand_method=None cands_num=None' % (ex_name, data, side, k, percent, width, max_value))
            if ret == 256:
                print 'KeyboardInterrupt'
                exit()
for width in [7, 5]:
#for width in [3, 4, 8, 9, 2, 10]:
    #for k in [100, 150, 200, 250]:
    for k in [250, 200, 150, 100]:
        for percent in [0.0001, 0.0005, 0.001, 0.002, 0.003, 0.004]:
            #for max_value in frange(0.1, 0.25, 0.05):
            for max_value in [0.3]:
                ret = os.system('python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 percent=%f width=%d alpha=0.9 max_value=%f topic_model=pLSA sel_cand_method=None cands_num=None' % (ex_name, data, side, k, percent, width, max_value))
                if ret == 256:
                    print 'KeyboardInterrupt'
                    exit()
# SVD None 15
side = 15
i = 0
for width in [6, 5, 4]:
    #for k in [100, 150, 200, 250]:
    for k in [200, 150, 100]:
        for percent in frange(0.05, 0.025, -0.003):
            #for max_value in frange(0.2, 0.35, 0.05):
            for max_value in [0.3]:
                ret = os.system('python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 percent=%f width=%d alpha=0.9 max_value=%f topic_model=SVD sel_cand_method=None cands_num=None' % (ex_name, data, side, k, percent, width, max_value))
                if ret == 256:
                    print 'KeyboardInterrupt'
                    exit()
# pLSA None
side = 15
#for width in [6, 5, 4]:
for width in [5, 4]:
    #for k in [100, 150, 200, 250]:
    for k in [250, 200]:
        for percent in [0.0001, 0.0005, 0.001, 0.002, 0.003, 0.004]:
        #for percent in [0.0005, 0.0001, 0.0003, 0.0005, 0.001, 0.003]:
            #for max_value in frange(0.2, 0.35, 0.05):
            for max_value in [0.3]:
                ret = os.system('python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 percent=%f width=%d alpha=0.9 max_value=%f topic_model=pLSA sel_cand_method=None cands_num=None' % (ex_name, data, side, k, percent, width, max_value))
                if ret == 256:
                    print 'KeyboardInterrupt'
                    exit()
# SVD None
side = 20
i = 0
for width in [3, 4, 5]:
    #for k in [100, 150, 200, 250]:
    for k in [250, 200, 150, 100]:
        for percent in frange(0.06, 0.025, -0.003):
            for max_value in frange(0.15, 0.4, 0.05):
                ret = os.system('python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 percent=%f width=%d alpha=0.9 max_value=%f topic_model=SVD sel_cand_method=None cands_num=None' % (ex_name, data, side, k, percent, width, max_value))
                if ret == 256:
                    print 'KeyboardInterrupt'
                    exit()
# pLSA None
side = 20
for width in [3, 4, 5]:
    #for k in [100, 150, 200, 250]:
    for k in [250, 200, 150, 100]:
        for percent in [0.0001, 0.0005, 0.001, 0.002, 0.003, 0.004]:
        #for percent in [0.0005, 0.0001, 0.0003, 0.0005, 0.001, 0.003]:
            for max_value in frange(0.2, 0.35, 0.05):
                ret = os.system('python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 percent=%f width=%d alpha=0.9 max_value=%f topic_model=pLSA sel_cand_method=None cands_num=None' % (ex_name, data, side, k, percent, width, max_value))
                if ret == 256:
                    print 'KeyboardInterrupt'
                    exit()
'''
