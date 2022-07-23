import numpy as np

from datetime import timedelta
from functools import reduce
import operator

def percentile(n):
    def percentile_(x):
        return np.percentile(x, n)

    percentile_.__name__ = 'percentile_%s' % n
    return percentile_


def share_with_nan(value, base):
    if base == 0:
        return float('NaN')
    else:
        return value / base


# Function to flip lat/lon coordinates
def reverse_lat_lon(coords):
    geom = []
    for lat_lon in coords:
        geom.append([lat_lon[1], lat_lon[0]])
    return geom

# -*- coding: utf-8 -*-


class DatetimeRange:
    def __init__(self, start_date, end_date):
        if end_date < start_date:
            raise ValueError('{!r} < {!r}'.format(end_date, start_date))

        self.start_date = start_date
        self.end_date = end_date

    def __contains__(self, date):
        """ (start_date, end_date] """
        return self.start_date <= date < self.end_date

    def __eq__(self, other):
        return (
            (self.start_date == other.start_date) and
            (self.end_date == other.end_date)
        )

    def __and__(self, other):
        """ Return overlapping range for dr1 & dr2. """

        dates = sorted([
            self.start_date,
            self.end_date,
            other.start_date,
            other.end_date
        ])

        if self.end_date < other.start_date or self.start_date > other.end_date:
            return

        return DatetimeRange(dates[1], dates[2])

    def __sub__(self, other):
        """ Return list with ranges """
        intersection = self & other

        if intersection is None:
            return [self]

        result = []

        if intersection.start_date != self.start_date:
            result.append(DatetimeRange(self.start_date, intersection.start_date))

        if intersection.end_date != self.end_date:
            result.append(DatetimeRange(intersection.end_date, self.end_date))

        return result

    @property
    def delta(self):
        return self.end_date - self.start_date

    def __repr__(self):
        return '<DatetimeRange({!r}, {!r}>'.format(
            self.start_date, self.end_date
        )


def coverage(primary_range, sub_ranges):
    left = [primary_range]

    for sub_range in sub_ranges:
        new_left = []

        for left_range in left:
            new_left.extend(left_range - sub_range)

        left = new_left

        if not left:
            break

    delta_left = reduce(
        operator.add,
        (i.delta for i in left),
        timedelta()
    )

    return primary_range.delta - delta_left


def coverage_sum(primary_range, sub_ranges):
    time_coverage = 0

    for sub_range in sub_ranges:
        coverage_sub_range = primary_range & sub_range
        if coverage_sub_range is not None:
            time_coverage += coverage_sub_range.delta.total_seconds()

    return time_coverage
