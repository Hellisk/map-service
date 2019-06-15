#!/usr/bin/env python

import os

import taskmanager as tm

from plsa import pLSA
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


@tm.task(feat, int, int)
def train(data, maxiter=500, debug=True):
    td, idf, vocab = data
    td = td[:, :-1]
    plsa = pLSA()
    plsa.debug = debug
    return plsa.train(td, 10, maxiter)


@tm.task(feat, int, int)
def average_train(data, maxiter=500, debug=True):
    td, idf, vocab = data
    td = td[:, :-1]
    plsa = pLSA()
    plsa.debug = debug
    return plsa.average_train(5)(td, 10, maxiter)


@tm.task(feat, train, int, int)
def folding_in(data, model, maxiter=50, debug=True):
    td, idf, vocab = data
    d = td[:, -1]
    plsa = pLSA(model)
    plsa.debug = debug
    print plsa.folding_in(d, maxiter)


@tm.nocache
@tm.task(train)
def document_topics(model):
    plsa = pLSA(model)
    print plsa.document_topics()


@tm.nocache
@tm.task(train)
def document_cluster(model):
    plsa = pLSA(model)
    print plsa.document_cluster()


@tm.nocache
@tm.task(train)
def word_topics(model):
    plsa = pLSA(model)
    print plsa.word_topics()


@tm.nocache
@tm.task(train)
def word_cluster(model):
    plsa = pLSA(model)
    print plsa.word_cluster()


@tm.nocache
@tm.task(train)
def unigram_smoothing(model):
    plsa = pLSA(model)
    print plsa.unigram_smoothing()


@tm.nocache
@tm.task(feat, train, int)
def topic_labels(data, model, N=15):
    td, idf, vocab = data
    plsa = pLSA(model)
    inv_vocab = inverse_vocab(vocab)
    print plsa.topic_labels(inv_vocab, N)


@tm.nocache
@tm.task(feat, train)
def global_weights(data, model):
    td, idf, vocab = data
    plsa = pLSA(model)
    print plsa.global_weights(idf)


def main():
    import sys

    try:
        tm.TaskManager.OUTPUT_FOLDER = "./tmp"
        tm.run_command(sys.argv[1:])
    except tm.TaskManagerError, m:
        print >> sys.stderr, m


if __name__ == "__main__":
    main()
