def laplace(td):
    """
    Laplace smoothing a.k.a as "add one" smoothing.

    In a bayesian setting, this corresponds to a uniform prior on events.

    It has a tendency to over-estimate probabilities of unseen terms for large
    matrices.

    td: V x X term document matrix
    """
    V, X = td.shape
    return (1.0 + td) / (V + td.sum(axis=0))


def lidstone(td, lambda_=0.5):
    V, X = td.shape
    return (lambda_ + td) / (V * lambda_ + td.sum(axis=0))
