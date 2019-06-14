#!/usr/bin/env python

import os

import numpy as np
import taskmanager as tm

from lsa import LSA
from tfidf.preprocessing import read_files, preprocess_documents
from tfidf.tfidf import *


@tm.task(str)
def feat(folder):
    docs = preprocess_documents(read_files(os.path.join(folder, "*.txt")))
    assert (len(docs) > 0)
    # stemmer = PorterStemmer()
    # docs = stemmer.stem_documents(docs)
    td_dict, vocab = tc(docs)
    td = to_sparse_matrix(td_dict, vocab).toarray()
    idf = to_vector(idf_from_tc(td_dict), vocab)
    print "term-document matrix size", td.shape
    return td, idf, vocab


@tm.task(feat)
def train(data):
    td, idf, vocab = data
    td = td[:, :-1]
    lsa = LSA()
    return lsa.train(td, Z=10)


@tm.task(feat, train)
def folding_in(data, model):
    td, idf, vocab = data
    d = td[:, -1]
    lsa = LSA(model)
    print lsa.folding_in(d).shape


@tm.nocache
@tm.task(train)
def document_topics(model):
    lsa = LSA(model)
    print lsa.document_topics().shape


@tm.nocache
@tm.task(train)
def word_topics(model):
    lsa = LSA(model)
    print lsa.word_topics().shape


@tm.nocache
@tm.task(feat, train)
def unigram_smoothing(data, model):
    td, idf, vocab = data
    td = td[:, :-1]
    lsa = LSA(model)
    res = lsa.unigram_smoothing()
    print res.shape
    print np.abs(td - res).sum() / float(res.shape[0] * res.shape[1])


@tm.nocache
@tm.task(feat, train, int)
def topic_labels(data, model, N=15):
    td, idf, vocab = data
    lsa = LSA(model)
    inv_vocab = inverse_vocab(vocab)
    print lsa.topic_labels(inv_vocab, N)


def main():
    import sys

    try:
        tm.TaskManager.OUTPUT_FOLDER = "./tmp"
        tm.run_command(sys.argv[1:])
    except tm.TaskManagerError, m:
        print >> sys.stderr, m


if __name__ == "__main__":
    main()
