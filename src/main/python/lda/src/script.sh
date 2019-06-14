python sacred_trajmap.py with ex_name=new_eval data=chicago side=10 k=250 percent=0.037 width=6 max_value=0.1 topic_model=SVD sel_cand_method=None cands_num=None
#python sacred_trajmap.py with ex_name=new_eval data=chicago side=10 k=150 percent=0.018 width=7 max_value=0.22 topic_model=pLSA sel_cand_method=max cands_num=259
#python sacred_trajmap.py with ex_name=shanghai data=minsh_1000 side=15 k=250 percent=0.036 width=7 max_value=0.1 topic_model=SVD sel_cand_method=sum cands_num=350
#python sacred_trajmap.py with ex_name=shanghai data=minsh_1000 side=10 k=250 percent=0.036 width=7 max_value=0.1 topic_model=pLSA sel_cand_method=max cands_num=350
#python sacred_trajmap.py with ex_name=shanghai data=minsh_1000 side=10 k=250 percent=0.036 width=7 max_value=0.1 topic_model=SVD sel_cand_method=sum cands_num=350
#python sacred_trajmap.py with ex_name=shanghai data=minsh_1000 side=10 k=250 percent=0.036 width=7 max_value=0.1 topic_model=SVD sel_cand_method=max cands_num=350

#python sacred_trajmap.py with ex_name=new_eval data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=6 alpha=0.9 max_value=0.20 topic_model=SVD
#python sacred_trajmap.py with ex_name=chicago data=minsh_5000 side=15 k=200 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2 topic_model=SVD
#python sacred_trajmap.py with ex_name=chicago data=minsh_10000 side=15 k=200 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2 topic_model=SVD

if false ; then
python sacred_trajmap.py with ex_name=chicago data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.4 topic_model=SVD
python sacred_trajmap.py with ex_name=chicago data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.37 topic_model=SVD
python sacred_trajmap.py with ex_name=chicago data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.35 topic_model=SVD
python sacred_trajmap.py with ex_name=chicago data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.32 topic_model=SVD

python sacred_trajmap.py with ex_name=minsh data=minsh_1000 side=20 k=200 ratio=0.9 percent=0.02 width=4 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=minsh data=minsh_1000 side=10 k=200 ratio=0.9 percent=0.02 width=4 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=minsh data=minsh_1000 side=15 k=200 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=minsh data=minsh_1000 side=10 k=200 ratio=0.9 percent=0.02 width=7 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=minsh data=minsh_1000 side=5 k=200 ratio=0.9 percent=0.02 width=12 alpha=0.9 max_value=0.2

## TrajMap
# trajmap_width
python sacred_trajmap.py with ex_name=trajmap_width data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_width data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=6 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_width data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=7 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_width data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=4 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_width data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=3 alpha=0.9 max_value=0.2

python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=10 k=150 ratio=0.9 percent=0.02 width=3 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=10 k=100 ratio=0.9 percent=0.02 width=4 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=10 k=150 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=10 k=150 ratio=0.9 percent=0.02 width=6 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=10 k=150 ratio=0.9 percent=0.02 width=7 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=10 k=150 ratio=0.9 percent=0.02 width=8 alpha=0.9 max_value=0.2


# trajmap_k
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=15 k=250 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=15 k=200 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=15 k=150 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=15 k=100 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=15 k=50 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=trajmap_k data=chicago side=15 k=25 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2

## MinSh

python sacred_trajmap.py with ex_name=minsh data=minsh_1000 side=15 k=200 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=minsh data=minsh_1000 side=10 k=200 ratio=0.9 percent=0.02 width=7 alpha=0.9 max_value=0.2
python sacred_trajmap.py with ex_name=minsh data=minsh_1000 side=5 k=200 ratio=0.9 percent=0.02 width=12 alpha=0.9 max_value=0.2
fi
#python sacred_trajmap.py with ex_name=minsh data=minsh1000 side=15 k=200 ratio=0.9 percent=0.02 width=5 alpha=0.9 max_value=0.2

#python sacred_trajmap.py with data=chicago side=20 k=100 ratio=0.9 percent=0.02 width=5 alpha=0.9

#python sacred_trajmap.py with data_file="../Data/Chicago/chicago.pickle" side=15 k=50 percent=0.02 width=4 alpha=0.9
#python sacred_trajmap.py with data_file="../Data/Chicago/chicago.pickle" side=10 k=50 percent=0.02 width=4 alpha=0.9
#python sacred_trajmap.py with data_file="../Data/Chicago/chicago.pickle" side=5 k=50 percent=0.02 width=4 alpha=0.9