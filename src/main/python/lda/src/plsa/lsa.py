# -*- coding: utf-8 -*-

# Copyright (C) 2010 Mathieu Blondel
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along
#  with this program; if not, write to the Free Software Foundation, Inc.,
#  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

import numpy as np
from numpy import linalg

"""
Notation:

    W: vocabulary size
    D: number of documents
    Z: number of topics/concepts

"""


class LSA(object):

    def __init__(self, model=None):
        """
        model: a model, as returned by get_model() or train().
        """
        if model is not None: self.set_model(model)

    def train(self, td, Z):
        """
        Train the model.

        td: a W x D term-document matrix of term-counts or term-frequencies.
        """
        U, S, Vt = linalg.svd(td, full_matrices=False)

        self.U = U[:, :Z]  # W x Z matrix
        self.S = S[:Z]  # Z vector
        self.Vt = Vt[:Z, :]  # Z x D matrix

        Ut = self.U.transpose()
        # V = self.Vt.transpose()
        # pre-compute S^-1 U^t as it is necessary for document folding in
        self.invS_Ut = np.dot(linalg.inv(np.diag(self.S)), Ut)

        return self.get_model()

    def document_topics(self):
        """
        Get the concept-document matrix.

        Return: a Z x D matrix.

        Note: This can be seen as a dimensionality reduction since a Z x D
        matrix is obtained from a W x D matrix, where Z << W.
        """
        return self.Vt

    def word_topics(self):
        """
        Get the concept-term matrix.

        Return: a Z x W matrix.
        """
        return self.U.transpose()

    def topic_labels(self, inv_vocab, N=10):
        """
        For each topic z, find the top N words.

        inv_vocab: a term-index => term-string dictionary

        Return: Z lists of N words.
        """
        W, Z = self.U.shape
        ret = []
        for z in range(Z):
            ind = np.argsort(self.U[:, z])[-N:][::-1]
            ret.append([inv_vocab[i] for i in ind])
        return ret

    def unigram_smoothing(self):
        """
        Compute the original matrix with noise removed.

        Return: a W x D matrix.
        """
        return np.dot(self.U, np.dot(np.diag(self.S), self.Vt))

    def folding_in(self, d):
        """
        Compute the concept vector for a new document d.

        d: a W-array of term-counts or term-frequencies.

        Return: a Z-array.
        """
        return np.dot(self.invS_Ut, d)

    def get_model(self):
        return (self.U, self.S, self.Vt, self.invS_Ut)

    def set_model(self, model):
        self.U, self.S, self.Vt, self.invS_Ut = model
