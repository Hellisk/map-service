from __future__ import division  # enable float division by default

import numpy as np
from numpy import log2

from tfidf import vocab_array, vocab_dict


def get_counts(td, delta, t, c):
    in_c = delta[:, c] == 1  # |X|-array
    not_in_c = delta[:, c] == 0  # |X|-array
    N11 = np.sum(td[t, in_c] > 0)  # contains t and is in c
    N01 = np.sum(td[t, in_c] == 0)
    N10 = np.sum(td[t, not_in_c] > 0)
    N00 = np.sum(td[t, not_in_c] == 0)
    return (N11, N01, N10, N00)


def get_group_counts(N11, N01, N10, N00):
    N1_ = N10 + N11
    N_1 = N11 + N01
    N0_ = N01 + N00
    N_0 = N10 + N00
    N = N10 + N11 + N01 + N00
    return (N1_, N_1, N0_, N_0, N)


def mutual_information(td, delta):
    """
    td: a term-document V x |X| matrix
    delta: |X| x M matrix where delta(i,j) = 1 if document i belongs to class j

    output: a V x M matrix of scores I(t,c)
    """
    V, X = td.shape
    X_, M = delta.shape

    assert (X == X_)

    I = np.zeros((V, M), dtype=np.double)

    for t in range(V):
        for c in range(M):
            N11, N01, N10, N00 = get_counts(td, delta, t, c)
            N1_, N_1, N0_, N_0, N = get_group_counts(N11, N01, N10, N00)

            # FIXME: how to deal with log2(0) when the numerator is 0?

            I[t, c] = N11 / N * log2((N * N11) / (N1_ * N_1)) + \
                      N01 / N * log2((N * N01) / (N0_ * N_1)) + \
                      N10 / N * log2((N * N10) / (N1_ * N_0)) + \
                      N00 / N * log2((N * N00) / (N0_ * N_0))

    return I


def chi2(td, delta):
    """
    td: a term-document V x |X| matrix
    delta: |X| x M matrix where delta(i,j) = 1 if document i belongs to class j

    output: a V x M matrix of scores chi2(t,c)
    """
    V, X = td.shape
    X_, M = delta.shape

    assert (X == X_)

    chi2m = np.zeros((V, M), dtype=np.double)

    for t in range(V):
        for c in range(M):
            N11, N01, N10, N00 = get_counts(td, delta, t, c)
            N1_, N_1, N0_, N_0, N = get_group_counts(N11, N01, N10, N00)

            chi2m[t, c] = (N * (N11 * N00 - N10 * N01) ** 2) / (N1_ * N_1 * N0_ * N_0)

    return chi2m


def select_max(td, vocab, A, K):
    """
    Select the best K/M features for each of the M classes

    td: a term-document V x |X| matrix
    delta: |X| x M matrix where delta(i,j) = 1 if document i belongs to class j

    A: matrix returned by chi2 or mutual_information

    output: the new reduced term-document matrix and the new vocabulary dict
    """
    V, M = A.shape

    d = {}

    for m in range(M):
        k = 1
        # best features which are not selected yet
        best_feat = [a for a in A[:, m].argsort()[::-1] if not a in d]
        d.update(dict((a, 1) for a in best_feat[:int(K / M)]))

    best_feat = np.array(d.keys())
    varr = vocab_array(vocab)

    return td[best_feat, :], vocab_dict(varr[best_feat])


def select_avg(td, vocab, A, K, weights=None):
    """
    Select the best K features by averaging the scores

    td: a term-document V x |X| matrix
    delta: |X| x M matrix where delta(i,j) = 1 if document i belongs to class j

    A: matrix returned by chi2 or mutual_information

    output: the new reduced term-document matrix and the new vocabulary dict
    """
    a = np.average(A, axis=1, weights=weights)
    best_feat = a.argsort()[::-1][:K]
    varr = vocab_array(vocab)
    return td[best_feat, :], vocab_dict(varr[best_feat])
