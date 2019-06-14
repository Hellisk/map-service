import glob
import re
import string


def strip_punctuation(s):
    return re.sub("([%s]+)" % string.punctuation, " ", s)


def strip_punctuation2(s):
    return s.translate(string.maketrans("", ""), string.punctuation)


def strip_tags(s):
    # assumes s is already lowercase
    return re.sub(r"<([^>]+)>", "", s)


def strip_short(s, minsize=3):
    return " ".join([e for e in s.split() if len(e) >= minsize])


def strip_numeric(s):
    return re.sub(r"[0-9]+", "", s)


def strip_non_alphanum(s):
    # assumes s is already lowercase
    return re.sub(r"[^a-z0-9\ ]", " ", s)


def strip_multiple_whitespaces(s):
    return re.sub(r"(\s|\\n|\\r|\\t)+", " ", s)
    # return s


def split_alphanum(s):
    s = re.sub(r"([a-z]+)([0-9]+)", r"\1 \2", s)
    return re.sub(r"([0-9]+)([a-z]+)", r"\1 \2", s)


STOPWORDS = """
a about again all almost also although always among an
and another any are as at 
be because been before being between both but by 
can could
did do does done due during
each either enough especially etc
for found from further
had has have having here how however
i if in into is it its itself
just
kg km
made mainly make may mg might ml mm most mostly must
nearly neither no nor not
obtained of often on our overall
perhaps pmid
quite
rather really regarding
seem seen several should show showed shown shows significantly
since so some such
than that the their theirs them then there therefore these they too
this those through thus to
upon use used using
various very
was we were what when which while with within without would will
"""

STOPWORDS = dict((w, 1) for w in STOPWORDS.strip().replace("\n", " ").split())


def remove_stopwords(s):
    return " ".join([w for w in s.split() if w not in STOPWORDS])


DEFAULT_FILTERS = [str.lower, strip_tags, strip_punctuation,
                   strip_multiple_whitespaces, strip_numeric, remove_stopwords, strip_short]


def preprocess_string(s, filters=DEFAULT_FILTERS):
    for f in filters:
        s = f(s)
    return s


def preprocess_documents(docs):
    return map(preprocess_string, docs)


def read_file(path):
    f = open(path)
    ret = f.read()
    return ret


def read_files(pattern):
    return map(read_file, glob.glob(pattern))
