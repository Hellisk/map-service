import os

import pp

data = 'minsh_5000'
# data = 'chicago'
# ex_name = 'topic2road_inc'
ex_name = 'topic2road'


def exam(ex_name, data, side, k, width, max_value, topic_model, maxiter, percent):
    cmd = 'python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=None width=%d alpha=0.9 max_value=%f topic_model=%s sel_cand_method=None cands_num=0 maxiter=%d percent=%f' % (
    ex_name, data, side, k, width, max_value, topic_model, maxiter, percent)
    ret = os.system(cmd)
    if ret == 256:
        print 'KeyboardInterrupt'
        exit()


def cmdline(ex_name, data, topic_model, side, k, width, max_value, maxiter, percent, alpha=0.9, beta=0.01, inc=0, true_pass=False,
            cut_length=0):
    cmd = 'python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=None width=%d alpha=%f beta=%f max_value=%f topic_model=%s sel_cand_method=None cands_num=0 maxiter=%d percent=%f inc=%d true_pass=%s cut_length=%d' % (
    ex_name, data, side, k, width, alpha, beta, max_value, topic_model, maxiter, percent, inc, str(true_pass), cut_length)
    return cmd


def execute(cmd):
    print cmd
    ret = os.popen(cmd).read()
    # ret = os.system(cmd)
    '''
    if ret == 256:
        print 'KeyboardInterrupt'
        exit()
    return ret
    '''
    print ret
    return 1


'''
def execute(cmd):
    #ret = os.system('echo hi')
    ret = os.popen('echo hi').read()
    print ret
    return 1
'''

# execute(cmdline(ex_name, data, topic_model='pLSA', side=15, k=10, width=5, max_value=0.8, maxiter=60, percent=0.2, cut_length=500))

# exit()
# execute(cmdline(ex_name, data, topic_model='pLSA_cut', side=15, k=10, width=5, max_value=0.8, maxiter=60, percent=0.2, inc=2))
# execute(cmdline(ex_name, data, topic_model='pLSA_cut', side=15, k=10, width=4, max_value=1.0, maxiter=20, percent=0.2))

ppservers = ()
job_server = pp.Server(ppservers=ppservers)
job_server.set_ncpus(6)
print "Starting pp with", job_server.get_ncpus(), "workers"

# Parameter K
side = 15
data = 'minsh_10000'
topic_model = 'LDA'
jobs = []
idx = 0
for width in [4]:
    for k in [10, 20, 40, 60, 80, 120, 140, 160, 180]:
        for inc in [0]:
            for percent in [0.1]:
                for max_value in [1.0]:
                    for maxiter in [40]:
                        cmd = cmdline(ex_name, data, topic_model=topic_model, side=side, k=k, width=width, max_value=max_value,
                                      maxiter=maxiter, percent=percent, alpha=0.2, beta=0.1, inc=0)
                        # job = job_server.submit(exam, (ex_name, data, side, k, width, max_value, topic_model, maxiter, percent,))
                        idx += 1
                        jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])
for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d' % (idx, len(jobs), cmd, job())

job_server.print_stats()

'''
ex_name = 'topic2road'
#ex_name = 'visualization'
# minsh
side = 15
data = 'minsh_10000'
topic_model = 'LDA'
jobs = []
idx = 0
percent = 0.1
max_value = 1.0
maxiter = 40
for width in [3]:
    for k in [100]:
        for data in ['minsh_10000']:
        #for data in ['chicago']:
            for percent in [0.1]:
                for side, width in [(10, 6), (20, 3), (30, 2), (40, 2), (50, 1), (60, 1)]:
                    for max_value in [1.0]:
                        cmd = cmdline(ex_name, data, topic_model=topic_model, side=side, k=k, width=width, max_value=max_value, maxiter=maxiter, percent=percent, alpha=0.2, beta=0.1, inc=0)
                        #ret = os.system(cmd)
                        #print ret
                        job = job_server.submit(exam, (ex_name, data, side, k, width, max_value, topic_model, maxiter, percent,))
                        idx += 1
                        jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])
for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d'%(idx,len(jobs), cmd, job())

job_server.print_stats()

ex_name = 'topic2road'
ex_name = 'visualization'
# minsh
side = 15
#data = 'maxsh_100000'
topic_model = 'pLSA'
jobs = []
idx = 0
percent = 0.1
max_value = 1.0
maxiter = 40
for width in [3, 4, 5]:
    for k in [20, 40, 60]:
        for data in ['minsh_6000']:
        #for data in ['chicago']:
            for percent in [0.1, 0.05]:
                for side in [10, 15]:
                    for max_value in [1.0, 1.2]:
                        cmd = cmdline(ex_name, data, topic_model=topic_model, side=side, k=k, width=width, max_value=max_value, maxiter=maxiter, percent=percent, alpha=0.2, beta=0.1, inc=0)
                        #ret = os.system(cmd)
                        #print ret
                        job = job_server.submit(exam, (ex_name, data, side, k, width, max_value, topic_model, maxiter, percent,))
                        idx += 1
                        jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])
for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d'%(idx,len(jobs), cmd, job())

job_server.print_stats()
'''

'''
# maxsh
data = 'maxsh_100000'
topic_model = 'pLSA'
jobs = []
idx = 0
side = 20
percent = 0.1
max_value = 1.0
for width in [3, 4]:
    for k in [300, 400]:
        for data in ['maxsh_20000', 'maxsh_40000', 'maxsh_60000', 'maxsh_80000']:
            for side in [20, 30]:
                for max_value in [1.0]:
                    for maxiter in [40]:
                        cmd = cmdline(ex_name, data, topic_model=topic_model, side=side, k=k, width=width, max_value=max_value, maxiter=maxiter, percent=percent, alpha=0.2, beta=0.1, inc=0)
                        #ret = os.system(cmd)
                        #print ret
                        job = job_server.submit(exam, (ex_name, data, side, k, width, max_value, topic_model, maxiter, percent,))
                        idx += 1
                        jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])
for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d'%(idx,len(jobs), cmd, job())

job_server.print_stats()
'''

'''
# parameter h
side = 15
data = 'minsh_10000'
topic_model = 'pLSA'
jobs = []
idx = 0
for width in [2, 3, 4, 5, 6, 7, 8]:
    for k in [100]:
        for inc in [0]:
            for percent in [0.1]:
                for max_value in [1.0]:
                    for maxiter in [40]:
                        cmd = cmdline(ex_name, data, topic_model=topic_model, side=side, k=k, width=width, max_value=max_value, maxiter=maxiter, percent=percent, alpha=0.2, beta=0.1, inc=inc)
                        #job = job_server.submit(exam, (ex_name, data, side, k, width, max_value, topic_model, maxiter, percent,))
                        idx += 1
                        jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])
for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d'%(idx,len(jobs), cmd, job())

job_server.print_stats()

# parameter side
data = 'minsh_10000'
topic_model = 'pLSA'
jobs = []
idx = 0
width = 4
for side in [5, 10, 15, 20, 25, 30, 35, 40]:
#for side in [40]:
    for k in [100]:
        for inc in [0]:
            for percent in [0.1]:
                for max_value in [1.0]:
                    for maxiter in [40]:
                        cmd = cmdline(ex_name, data, topic_model=topic_model, side=side, k=k, width=width, max_value=max_value, maxiter=maxiter, percent=percent, alpha=0.2, beta=0.1, inc=inc)
                        #job = job_server.submit(exam, (ex_name, data, side, k, width, max_value, topic_model, maxiter, percent,))
                        idx += 1
                        jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])
for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d'%(idx,len(jobs), cmd, job())

job_server.print_stats()

data = 'maxsh_1000'
topic_model = 'pLSA'
jobs = []
idx = 0
width = 4
for side in [5, 10, 15, 20, 25, 30, 35, 40]:
    for k in [100]:
        for inc in [0]:
            for percent in [0.1]:
                for max_value in [1.0]:
                    for maxiter in [40]:
                        cmd = cmdline(ex_name, data, topic_model=topic_model, side=side, k=k, width=width, max_value=max_value, maxiter=maxiter, percent=percent, alpha=0.2, beta=0.1, inc=inc)
                        #job = job_server.submit(exam, (ex_name, data, side, k, width, max_value, topic_model, maxiter, percent,))
                        idx += 1
                        jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])
for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d'%(idx,len(jobs), cmd, job())

job_server.print_stats()
'''

'''
side = 15
topic_model = 'pLSA_cut'
jobs = []
idx = 0
for width in [5]:
    for k in [10]:
        for inc in [0,1,2]:
            for percent in [0.3, 0.2, 0.1]:
                for max_value in [0.7, 0.8, 0.9]:
                    for maxiter in [60]:
                        cmd = 'python sacred_trajmap.py with ex_name=%s data=%s side=%d k=%d ratio=1.0 width=%d alpha=0.9 max_value=%f topic_model=%s sel_cand_method=None cands_num=0 maxiter=%d percent=%f inc=%d' % (ex_name, data, side, k, width, max_value, topic_model, maxiter, percent, inc)
                        #job = job_server.submit(exam, (ex_name, data, side, k, width, max_value, topic_model, maxiter, percent,))
                        idx += 1
                        jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])
for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d'%(idx,len(jobs), cmd, job())

job_server.print_stats()

side = 15
topic_model = 'LDA'
jobs = []
idx = 0
for width in [4,5]:
    for k in [150, 200, 250]:
        for percent in [0.2, 0.1]:
            for max_value in [1.0, 0.9]:
                for maxiter in [40, 60]:
                    for inc in [1,2]:
                        cmd = cmdline(ex_name, data, topic_model=topic_model, side=side, k=k, width=width, max_value=max_value, maxiter=maxiter, percent=percent, alpha=0.2, beta=0.1, inc=inc)
                        idx += 1
                        jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])

for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d'%(idx,len(jobs), cmd, job())

job_server.print_stats()

side = 15
topic_model = 'pLSA_cut'
idx = 0
jobs= []
for data in ['minsh_5000', 'minsh_10000']:
    for width in [4,5]:
        for k in [20, 40, 60]:
            for percent in [0.2, 0.3, 0.1]:
                for max_value in [0.9, 1.0, 0.8]:
                    for maxiter in [20, 40, 60]:
                        cmd = cmdline(ex_name, data, topic_model=topic_model, side=side, k=k, width=width, max_value=max_value, maxiter=maxiter, percent=percent)
                        idx += 1
                        jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])

for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d'%(idx,len(jobs), cmd, job())

job_server.print_stats()
'''
