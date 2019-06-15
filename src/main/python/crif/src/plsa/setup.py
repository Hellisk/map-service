# -*- coding: utf-8 -*-
from distutils.core import setup
from distutils.extension import Extension

import numpy as np
from Cython.Distutils import build_ext

setup(
    name="plsa",
    cmdclass={'build_ext': build_ext},
    ext_modules=[Extension("_plsa", ["_plsa.pyx", "plsa_train.c"],
                           include_dirs=[np.get_include(), '.'],
                           extra_compile_args=['-O3'])],
    py_modules=['plsa', ],
)
