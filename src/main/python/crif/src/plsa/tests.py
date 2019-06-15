# -*- coding: utf-8 -*-

import unittest

import numpy as np

from plsa import normalize


class UtilsTest(unittest.TestCase):

    def test_normalize(self):
        # 1d-case
        a = normalize(np.random.random(10))
        self.assertAlmostEquals(1.0, np.sum(a))

        # 1d-case where all values are 0
        a = normalize(np.array([0.0] * 10))
        self.assertEquals(0.0, np.sum(a))

        # 2d-case, axis 1
        M = normalize(np.random.random(20).reshape(2, 10), axis=1)
        res = M.sum(axis=1)  # a 2-array
        self.assertAlmostEquals(1.0, res[0])
        self.assertAlmostEquals(1.0, res[1])

        # 2d-case, axis 0
        M = normalize(np.random.random(20).reshape(10, 2), axis=0)
        res = M.sum(axis=0)
        self.assertAlmostEquals(1.0, res[0])
        self.assertAlmostEquals(1.0, res[1])

        # 2d-case, where the normalized axis has length 1
        M_ = np.random.random(10).reshape(10, 1)
        M = normalize(M_, axis=1)
        self.assertTrue(np.array_equal(M, M_))

        M_ = np.random.random(10).reshape(1, 10)
        M = normalize(M_, axis=0)
        self.assertTrue(np.array_equal(M, M_))


if __name__ == "__main__":
    unittest.main()
