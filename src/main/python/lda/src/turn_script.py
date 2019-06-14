import os

import pp

data = 'minsh_1000'
# data = 'chicago'
# ex_name = 'topic2road_inc'
ex_name = 'turn_hmm'


def cmdline(ex_name, data, limit=-1):
    cmd = 'python sacred_turn_hmm.py with ex_name=%s data=%s limit=%d' % (ex_name, data, limit)
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


ppservers = ()
job_server = pp.Server(ppservers=ppservers)
job_server.set_ncpus(6)
print "Starting pp with", job_server.get_ncpus(), "workers"

idx = 0
jobs = []
for data in ['minsh_2000', 'minsh_4000', 'minsh_6000', 'minsh_8000', 'minsh_10000']:
    # for data in ['minsh_1000']:
    for limit in [-1]:
        idx += 1
        cmd = cmdline(ex_name, data, limit)
    # execute(cmd)
    jobs.append([idx, cmd, job_server.submit(execute, (cmd,), )])

for idx, cmd, job in jobs:
    print '%d/%d: %s Return: %d' % (idx, len(jobs), cmd, job())

job_server.print_stats()
