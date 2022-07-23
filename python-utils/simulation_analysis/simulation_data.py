__author__ = 'Michael Wittmann'
__email__ = "michael.wittmann@tum.de"
__status__ = "Development"

import logging
import warnings
from pathlib import Path
from datetime import datetime, timedelta

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

import plotutils as pltutils
import utils as utils
from utils import DatetimeRange, coverage_sum
import pickle
logger = logging.getLogger(__name__)


class SimMetaData:
    __filename_prefix = 'simulationMetadata'

    def __init__(self, simulation_run_started: datetime = None,
                 simulation_run_finished: datetime = None,
                 simulation_start_time: datetime = None,
                 simulation_end_time: datetime = None,
                 dispatcher: str = 'Generic',
                 rebalancing_period: int = None,
                 dispatching_period: int = None,
                 number_of_requests: int = None,
                 fleet_size: int = None):
        self.simulation_run_started = simulation_run_started
        self.simulation_run_finished = simulation_run_finished
        self.simulation_start_time = simulation_start_time
        self.simulation_end_time = simulation_end_time
        self.dispatcher = dispatcher
        self.rebalancing_period = rebalancing_period
        self.dispatching_period = dispatching_period
        self.number_of_requests = number_of_requests
        self.fleet_size = fleet_size

    def __str__(self) -> str:
        return f'SimulationRun @{self.simulation_run_started} ' \
               f'[{self.simulation_start_time}-{self.simulation_end_time}, ' \
               f'{self.dispatcher}, RebalancingPeriod:{self.rebalancing_period} s, ' \
               f'DispatchingPeriod:{self.dispatching_period} s, FleetSize:{self.fleet_size}]'

    def label(self):
        # return f'{self.dispatcher} dp={self.dispatching_period} s, rb={self.rebalancing_period} s'
        return f"{self.dispatcher} |  " \
               f"{self.simulation_start_time}-" \
               f"{self.simulation_end_time} | " \
               f"#Vehicles: {self.fleet_size}"

    def get_year_week_str(self) -> str:
        year = self.simulation_start_time.isocalendar()[0]
        week = self.simulation_start_time.isocalendar()[1]
        return f'{year}_KW_{week}'

    @classmethod
    def from_csv(cls, file_path):
        """
        :param file_path: Path to output file from amodeus simulation
        """
        if not Path(file_path).is_file():
            logger.error(f'Unable to find folder {file_path}')
            raise FileNotFoundError(f'Unable to find folder {file_path}')

        df = pd.read_csv(file_path)
        return cls(
            datetime.strptime(df.simulation_run_started[0], '%Y-%m-%d %H:%M:%S'),
            datetime.strptime(df.simulation_run_finished[0], '%Y-%m-%d %H:%M:%S'),
            datetime.strptime(df.simulation_start_time[0], '%Y-%m-%d %H:%M:%S'),
            datetime.strptime(df.simulation_end_time[0], '%Y-%m-%d %H:%M:%S'),
            df.dispatcher[0],
            df.rebalancing_period[0],
            df.dispatching_period[0],
            df.number_of_requests[0],
            df.number_of_vehicles[0])

    @classmethod
    def from_output_folder(cls, output_folder_path):
        """
                :param output_folder_path: Path to output folder from amodeus simulation
                """
        if not Path(output_folder_path).is_dir():
            logger.error(f'Unable to find folder {output_folder_path}')
            raise FileNotFoundError(f'Unable to find folder {output_folder_path}')
        metadata_files = [f for f in Path(output_folder_path).glob(f"{cls.__filename_prefix}*") if f.is_file()]
        if metadata_files is None or len(metadata_files) != 1:
            logger.error(f"Unable to find metadata file in {output_folder_path}")
            raise FileNotFoundError(f"Unable to find metadata file in {output_folder_path}")
        return cls.from_csv(str(metadata_files[0]))

    @property
    def sim_duration(self):
        return (self.simulation_run_finished - self.simulation_run_started).total_seconds()


class SimulationData:
    __file_prefix_requests = 'requests'
    __file_prefix_fleet_status = 'fleetStatus'
    __file_prefix_trips = 'trips'

    def __init__(self, metadata: SimMetaData, trips: pd.DataFrame, requests: pd.DataFrame, fleet: pd.DataFrame):
        self.metadata = metadata
        self.trips = trips
        self.requests = requests
        self.fleet = fleet

    ###########################################################################################
    # Initial formatting functions
    ###########################################################################################
    @staticmethod
    def __format_trips_df(data: pd.DataFrame) -> pd.DataFrame:
        data.start_date_time = pd.to_datetime(data.start_date_time, format='%Y-%m-%d %H:%M:%S')
        data.end_date_time = pd.to_datetime(data.end_date_time, format='%Y-%m-%d %H:%M:%S')
        return data

    @staticmethod
    def __format_requests_df(data: pd.DataFrame) -> pd.DataFrame:
        data.timestamp_submitted = pd.to_datetime(data.timestamp_submitted, format='%Y-%m-%d %H:%M:%S')
        data.timestamp_dispatched = pd.to_datetime(data.timestamp_dispatched, format='%Y-%m-%d %H:%M:%S')
        data.timestamp_arrived = pd.to_datetime(data.timestamp_arrived, format='%Y-%m-%d %H:%M:%S')
        data.timestamp_started = pd.to_datetime(data.timestamp_started, format='%Y-%m-%d %H:%M:%S')
        return data

    @staticmethod
    def __format_fleet_status_df(data: pd.DataFrame) -> pd.DataFrame:
        data.time = pd.to_datetime(data.time, format='%Y-%m-%d %H:%M:%S')
        data.set_index(['time'], inplace=True)
        return data

    ###########################################################################################
    # Read and parse simulation result files
    ###########################################################################################
    @staticmethod
    def __df_from_csv_file(file_path: str) -> pd.DataFrame:
        """
        :param file_path: Path to output file from amodeus simulation
        """
        if not Path(file_path).is_file():
            logger.error(f'Unable to find file {file_path}')
            raise FileNotFoundError(f'Unable to find file {file_path}')
        return pd.read_csv(file_path)

    @classmethod
    def __df_from_output_folder(cls, output_folder_path: str, file_prefix: str, metadata: SimMetaData) -> pd.DataFrame:
        if not Path(output_folder_path).is_dir():
            logger.error(f'Unable to find folder {output_folder_path}')
            raise FileNotFoundError(f'Unable to find folder {output_folder_path}')
        file_path = f'{file_prefix}_{metadata.dispatcher}_{metadata.simulation_start_time:%Y%m%d-%H%M}' \
                    f'_{metadata.simulation_end_time:%Y%m%d-%H%M}.csv'
        return cls.__df_from_csv_file(str(Path(output_folder_path).joinpath(file_path)))

    @classmethod
    def from_output_folder(cls, output_folder_path: str, pkl_name: str = 'sim_data.pkl'):
        """
        :param output_folder_path: Path to output folder from amodeus simulation
        :param pkl_name: Name of the pickle file to speed up reading if processed multiple times
        """

        pickle_path = Path(output_folder_path).joinpath(pkl_name)
        if not Path(output_folder_path).is_dir():
            logger.error(f'Unable to find folder {output_folder_path}')
            raise FileNotFoundError(f'Unable to find folder {output_folder_path}')

        if not pickle_path.is_file():
            metadata = SimMetaData.from_output_folder(output_folder_path)
            trips = cls.__format_trips_df(
                cls.__df_from_output_folder(output_folder_path, cls.__file_prefix_trips, metadata))
            requests = cls.__format_requests_df(
                cls.__df_from_output_folder(output_folder_path, cls.__file_prefix_requests, metadata))
            fleet_status = cls.__format_fleet_status_df(
                cls.__df_from_output_folder(output_folder_path, cls.__file_prefix_fleet_status, metadata))
            sim_data = cls(metadata, trips, requests, fleet_status)

            pickle_file = open(pickle_path, "wb")
            pickle.dump(sim_data, pickle_file)
            return sim_data
        else:
            pickle_file = open(pickle_path, "rb")
            return pickle.load(pickle_file)

    def add_plot_footnote(self):
        plt.figtext(0.5, 0.01, self.metadata, ha="center", fontsize=5)

    def drop_initial_time_interval(self, delta_to_drop: timedelta = timedelta(hours=6)):
        min_date_time = self.metadata.simulation_start_time+delta_to_drop
        max_date_time = self.metadata.simulation_end_time
        self.metadata.simulation_start_time = min_date_time
        self.trips = self.trips[(self.trips.start_date_time >= min_date_time) &
                                (self.trips.end_date_time < max_date_time)]
        self.requests = self.requests[(self.requests.timestamp_submitted >= min_date_time) &
                                      (self.requests.timestamp_submitted < max_date_time)]
        self.fleet = self.fleet[min_date_time:max_date_time]

    ###########################################################################################
    # Accessors
    ###########################################################################################

    @property
    def service_quality(self):
        return ServiceQualityAccessor(self.requests)

    @property
    def fleet_statistics(self):
        return FleetStatisticsAccessor(self.fleet)

    @property
    def trip_statistics(self):
        return TripStatisticsAccessor(self.trips)

    @property
    def profitability(self):
        return ProfitabilityAccessor(self.trips)


@pd.api.extensions.register_dataframe_accessor("service_quality")
class ServiceQualityAccessor:
    def __init__(self, pandas_obj):
        self._validate(pandas_obj)
        self._obj = pandas_obj

    @staticmethod
    def _validate(obj):
        # verify there is a column latitude and a column longitude
        if 'timestamp_submitted' not in obj.columns or 'timestamp_dispatched' not in obj.columns \
                or 'timestamp_arrived' not in obj.columns or 'timestamp_started' not in obj.columns \
                or 'denied' not in obj.columns:
            raise AttributeError("Must have 'timestamp_submitted', 'timestamp_dispatched' "
                                 "'timestamp_arrived', 'timestamp_started' and 'denied' ")

    @property
    def waiting_times(self) -> pd.Series:
        waiting_times_series = self._obj.timestamp_arrived - self._obj.timestamp_submitted
        waiting_times_series.index = self._obj.timestamp_arrived
        waiting_times_series = waiting_times_series.sort_index()
        return waiting_times_series.dropna()

    @property
    def dispatching_times(self):
        return (self._obj.timestamp_dispatched - self._obj.timestamp_submitted).dropna()

    @property
    def boarding_times(self):
        return self._obj.timestamp_started - self._obj.timestamp_arrived

    @property
    def denied_requests(self):
        return self._obj.denied

    # TODO: check bin size rahter use fixed bins related to seconds instaed of just 100
    def plot_waiting_times_hist(self, ax=None, bins=100):
        if ax is None:
            fig, ax = pltutils.new_figure()
        counts, bins = np.histogram(self.waiting_times.dt.total_seconds(), bins=bins)
        ax.hist(bins[:-1], bins, weights=counts / self.waiting_times.shape[0])

    def plot_waiting_times_cdf(self, ax=None, bins=100):
        pltutils.plot_cdf(self.waiting_times.dt.total_seconds(), ax=ax, bins=bins)

    # TODO: Add waiting_times histogram plot

    # def plot(self):
    #     # plot this array's data on a map, e.g., using Cartopy
    #     pass


@pd.api.extensions.register_dataframe_accessor("fleet_statistics")
class FleetStatisticsAccessor:
    def __init__(self, pandas_obj):
        self._validate_fleet(pandas_obj)
        self._obj = pandas_obj

    @staticmethod
    def _validate_fleet(obj):
        # verify there is a column latitude and a column longitude
        if 'count_occupied' not in obj.columns \
                or 'count_approach' not in obj.columns \
                or 'count_rebalancing' not in obj.columns \
                or 'count_idle' not in obj.columns \
                or 'count_inactive' not in obj.columns:
            raise AttributeError("Must have 'count_occupied', 'count_approach' "
                                 "'count_rebalancing', 'count_idle' and 'count_inactive'")

    @property
    def count_active(self):
        return self._obj.count_occupied + self._obj.count_approach + self._obj.count_rebalancing + self._obj.count_idle

    @property
    def count_available(self):
        return self._obj.count_rebalancing + self._obj.count_idle

    def plot_active_vehicles(self, metadata: SimMetaData, ax=None, show_min=False, show_max=False,
                             show_mean=False, label=None, color=pltutils.tum_colors[0][0]):
        if ax is None:
            fig, ax = pltutils.new_figure()
        if label is None:
            label = metadata.label()
        ax.plot(self.count_active[:metadata.simulation_end_time][2:], marker='.', markersize=0.2,
                label=label, color=color)

        if show_mean:
            ax.axhline(self.count_active[:metadata.simulation_end_time][2:].mean(), linestyle='--', color='black',
                       label=f'Mittelwert={self.count_active.mean():.2f}')
        if show_max:
            ax.axhline(self.count_active[:metadata.simulation_end_time][2:].max(), linewidth=0.5,
                       linestyle='--', color='black', label=f'max={self.count_active.max()}')
        if show_min:
            ax.axhline(self.count_active[:metadata.simulation_end_time][2:].min(), linewidth=0.5,
                       linestyle='--', color='black', label=f'min={self.count_active.min()}')
        pltutils.format_plot_xlabels_days_6H(ax)
        ax.get_xaxis().set_label_text(None)
        ax.set_ylabel("Aktive Fahrzeuge")
        ax.set_ylim(0, int(self.count_active.max()) + 100)
        ax.legend()

    def plot_fleet_status(self, ax=None, stacked=True):
        if ax is None:
            fig, ax = pltutils.new_figure()
        if stacked:
            ax.stackplot(self._obj.index,
                         self._obj.iloc[:, :5].T)
        else:
            ax.plot(self._obj.index, self._obj.iloc[:, :5])

        pltutils.format_plot_xlabels_days_6H(ax)
        ax.legend(['Besetzt', 'In Anfahrt', 'Rebalancing', 'Frei', 'Abgemeldet'])
        ax.set_ylabel("Anzahl Fahrzeuge")
        # ax.set_ylim(0, 500)
        ax.set_title(f'Zeitraum: {self._obj.index[0]}-{self._obj.index[-1]}')

    def plot_fleet_utilization(self, ax=None, utilization_time=True, utilization_distance=False, availability=True,
                               frequency='15T'):
        if not (utilization_time and availability and utilization_distance):
            warnings.warn('Nothing to plot here. Either set utilization or availability to True')
        if ax is None:
            fig, ax = pltutils.new_figure()

        ts_availability = self.get_availability_over_time_ts(frequency)
        ts_occupancy_over_time = self.get_occupancy_over_time_ts(frequency)
        # ts_occupancy_over_distance = self.get_occupancy_over_distance_ts(frequency)

        if utilization_distance:
            # ax.plot(ts_occupancy_over_distance, label='Streckenauslasung')
            pass
        if utilization_time:
            ax.plot(ts_occupancy_over_time, label='zeitliche Flottenauslastung')
        if availability:
            ax.plot(ts_availability, label='Verfügbarkeit')
        ax.set_ylim(0, 1)
        pltutils.format_plot_xlabels_days_6H(ax)
        ax.set_title(f'Zeitraum: {self._obj.index[0]}-{self._obj.index[-1]}')
        ax.legend()

    def get_availability_over_time_ts(self, frequency='15T'):
        """
        Get the ratio of available vehicles in relation to all active vehicels. This differs from occupancy_over time
        as only vehicles which are able to be dispatched are taken into account. E. g. vehicles driving to a customer
        are not considered to be free.
        :param frequency: frequency used for resampling default:'15T'
        :return: pandas.Series with the mean fleet availability rate of the entire active fleet
        """
        availability_rate = self.count_available / self.count_active
        return availability_rate.resample(frequency).mean()

    def get_nonavailability_over_time_ts(self, frequency='15T'):
        """
        Get the ratio of available vehicles in relation to all active vehicels. This differs from occupancy_over time
        as only vehicles which are able to be dispatched are taken into account. E. g. vehicles driving to a customer
        are not considered to be free.
        :param frequency: frequency used for resampling default:'15T'
        :return: pandas.Series with the mean fleet availability rate of the entire active fleet
        """
        nonavailability_rate = 1 - (self.count_available / self.count_active)
        return nonavailability_rate.resample(frequency).mean()

    def get_occupancy_over_time_ts(self, frequency='15T'):
        """
        Get occupancy rate for active vehicles over time as time series with the specified frequency.
        :param frequency: frequency used for resampling default:'15T'
        :return: pandas.Series with the mean occupancy rate of the entire active fleet
        """
        occupancy_rate = self._obj.count_occupied / self.count_active
        return occupancy_rate.resample(frequency).mean()

    def get_occupancy_over_distance_ts(self, frequency='15T'):
        """
        Get occupancy rate for active vehicles over driven distance as time series with the specified frequency.
        :param frequency: frequency used for resampling default:'15T'
        :return: pandas.Series with the mean occupancy rate of the entire active fleet
        """
        ts_total = self._obj.trips.distance
        ts_total.index = self._obj.trips.end_date_time
        ts_total = ts_total.resample(frequency).sum()

        ts_occupied = self._obj.trips[self._obj.trips.taxi_status == 'occupied'].distance
        ts_occupied.index = self._obj.trips[self._obj.trips.taxi_status == 'occupied'].end_date_time
        ts_occupied = ts_occupied.resample(frequency).sum()
        ts_occupied = ts_occupied.reindex(ts_total.index, fill_value=0)
        df = ts_occupied / ts_total
        return df


@pd.api.extensions.register_dataframe_accessor("trip_statistics")
class TripStatisticsAccessor:
    def __init__(self, pandas_obj):
        self._validate(pandas_obj)
        self._obj = pandas_obj

    @staticmethod
    def _validate(obj):
        # verify there is a column latitude and a column longitude
        if 'trip_id' not in obj.columns \
                or 'request_id' not in obj.columns \
                or 'vehicle_id' not in obj.columns \
                or 'start_date_time' not in obj.columns \
                or 'end_date_time' not in obj.columns \
                or 'taxi_status' not in obj.columns \
                or 'start_location' not in obj.columns \
                or 'end_location' not in obj.columns \
                or 'distance' not in obj.columns \
                or 'costs' not in obj.columns \
                or 'revenue' not in obj.columns:
            raise AttributeError("Must have 'trip_id','request_id','vehicle_id','start_date_time','end_date_time',"
                                 "'taxi_status','start_location','end_location','distance','costs','revenue'")

    @property
    def pickups(self):
        return self._obj[self._obj.taxi_status == 'occupied'][['start_date_time', 'start_location', 'request_id']]

    @property
    def dropoffs(self):
        return self._obj[self._obj.taxi_status == 'occupied'][['end_date_time', 'end_location', 'request_id']]

    @property
    def duration(self):
        return self._obj.end_date_time - self._obj.start_date_time

    @property
    def avg_speed_kmh(self):
        return self.avg_speed_ms * 3.6

    @property
    def avg_speed_ms(self):
        return self._obj.distance / self.duration.dt.total_seconds()

    @property
    def distance_by_status(self):
        return self._obj.groupby(['taxi_status']).agg(
            {
                'distance':
                    [
                        'mean',
                        'std',
                        'median',
                        'min',
                        'max',
                        utils.percentile(5),
                        utils.percentile(95)
                    ]
            }
        )

    @property
    def distance_by_status_sum(self):
        df = self._obj.groupby(['taxi_status']).distance.sum().to_frame()
        df['share'] = df.distance / df.distance.sum()
        df = df.reindex(['approach', 'stay', 'occupied', 'rebalancing', 'off_service'], axis=0)
        return df

    @property
    def duration_by_status(self):
        df = self._obj.copy()
        df['duration'] = self.duration.dt.total_seconds()

        return df.groupby(['taxi_status']).agg(
            {
                'duration':
                    [
                        'mean',
                        'std',
                        'median',
                        'min',
                        'max',
                        utils.percentile(5),
                        utils.percentile(95)
                    ]
            }
        )

    @property
    def duration_by_status_sum(self):
        df = self._obj.copy()
        df['duration'] = self.duration.dt.total_seconds()
        df = df.groupby(['taxi_status']).duration.sum().to_frame()
        df['share'] = df.duration / df.duration.sum()
        # df = df.reindex(['approach', 'stay', 'occupied', 'rebalancing', 'off_service'], axis=0)
        return df

    def plot_distance_by_status(self, ax=None, unit='m'):
        if ax is None:
            fig, ax = pltutils.new_figure()
        df_plot = self._obj.copy()
        if unit == 'm':
            ax.set_ylabel('Distance in m')
        elif unit == 'km':
            df_plot.distance = df_plot.distance / 1000.0
            ax.set_ylabel('Distance in km')
        else:
            raise AttributeError("Only m or km is supported for unit")

        df_plot.boxplot(column=['distance'], by='taxi_status', showfliers=False, ax=ax)
        ax.set_title('Trip distances by status')

    def plot_duration_by_status(self, ax=None, unit='min', include_off_service=False):
        if ax is None:
            fig, ax = pltutils.new_figure()

        df_plot = self._obj.copy()
        if not include_off_service:
            df_plot = df_plot[df_plot['taxi_status'] != 'off service']

        df_plot['duration'] = (df_plot.end_date_time - df_plot.start_date_time).dt.total_seconds()

        if unit == 'min':
            ax.set_ylabel('Fahrtdauer in min')
            df_plot.duration = df_plot.duration / 60.0
        elif unit == 's':
            ax.set_ylabel('Fahrtdauer in s')

        elif unit == 'h':
            ax.set_ylabel('Fahrtdauer in h')
            df_plot.duration = df_plot.duration / 3600.0
        else:
            raise AttributeError("Only s, min or h is supported for unit")

        df_plot.boxplot(column=['duration'], by='taxi_status', showfliers=False, ax=ax)
        ax.set_title('Fahrtdauer nach Status')

    def plot_avg_speed_by_status(self, ax=None, unit='km/h'):
        if ax is None:
            fig, ax = pltutils.new_figure()
        df_plot = self._obj.copy()
        if unit == 'km/h':
            df_plot['avg_speed'] = df_plot.trip_statistics.avg_speed_kmh
            ax.set_ylabel('avg speed in km/h')
        elif unit == 'm/s':
            df_plot['avg_speed'] = df_plot.trip_statistics.avg_speed_ms
            ax.set_ylabel('avg speed in m/s')
        else:
            raise AttributeError("Only m/s or km/h is supported for unit")

        df_plot.boxplot(column=['avg_speed'], by='taxi_status', showfliers=False, ax=ax)
        ax.set_title('Avg speed by status')

    def get_pickup_ts(self, frequency='1H'):
        ts = self.pickups.start_date_time.copy()
        ts.index = ts
        return ts.resample(frequency).count().rename('pickups')

    def get_dropoff_ts(self, frequency='1H'):
        ts = self.dropoffs.end_date_time.copy()
        ts.index = ts
        return ts.resample(frequency).count().rename('dropoffs')


@pd.api.extensions.register_dataframe_accessor("profitability")
class ProfitabilityAccessor:
    def __init__(self, pandas_obj):
        self._validate(pandas_obj)
        self._obj = pandas_obj

    @staticmethod
    def _validate(obj):
        # verify there is a column latitude and a column longitude
        if 'trip_id' not in obj.columns \
                or 'request_id' not in obj.columns \
                or 'vehicle_id' not in obj.columns \
                or 'start_date_time' not in obj.columns \
                or 'end_date_time' not in obj.columns \
                or 'taxi_status' not in obj.columns \
                or 'start_location' not in obj.columns \
                or 'end_location' not in obj.columns \
                or 'distance' not in obj.columns \
                or 'costs' not in obj.columns \
                or 'revenue' not in obj.columns:
            raise AttributeError("Must have 'trip_id','request_id','vehicle_id','start_date_time','end_date_time',"
                                 "'taxi_status','start_location','end_location','distance','costs','revenue'")

    def get_fleet_total_revenue_ts(self, frequency='1H'):
        ts = self._obj['revenue'].copy()
        ts.index = self._obj['start_date_time']
        ts = ts.resample(frequency).sum().rename('fleet_turnover')
        return ts

    def get_fleet_total_costs_ts(self, frequency='1H'):
        ts = self._obj['costs'].copy()
        ts.index = self._obj['start_date_time']
        ts = ts.resample(frequency).sum().rename('fleet_costs')
        return ts

    def get_trip_revenue_statistics_ts(self, frequency='1H'):
        ts = self._obj[self._obj.taxi_status == 'occupied'].revenue.copy()
        ts.index = self._obj[self._obj.taxi_status == 'occupied'].start_date_time
        ts = ts.resample(frequency).agg(['mean',
                                         'std',
                                         'median',
                                         'min',
                                         'max',
                                         utils.percentile(5),
                                         utils.percentile(95)])
        return ts

    def get_trip_costs_statistics_ts(self, frequency='1H'):
        # filter out off_service this makes no sense here
        ts = self._obj[self._obj.taxi_status != 'off_service'].costs.copy()
        ts.index = self._obj[self._obj.taxi_status != 'off_service'].start_date_time
        ts = ts.resample(frequency).agg(['mean',
                                         'std',
                                         'median',
                                         'min',
                                         'max',
                                         utils.percentile(5),
                                         utils.percentile(95)])
        return ts

    def get_fleet_supply_hours_ts(self, date_range: pd.date_range):
        df = pd.Series(index=date_range, name='supply_hours', dtype='float64')
        df_active_trips = self._obj[self._obj.taxi_status != 'off service'][['start_date_time', 'end_date_time']]
        df_active_trips_tr = df_active_trips.apply(lambda x: DatetimeRange(x['start_date_time'], x['end_date_time']),
                                                   axis=1)

        for start_time in date_range:
            df.loc[start_time] = coverage_sum(DatetimeRange(start_time, start_time +
                                                            date_range.freq.delta),
                                              df_active_trips_tr)/3600
        return df

    def get_fleet_supply_hours_sum(self):
        df_active_trips = self._obj[self._obj.taxi_status != 'off service'][['start_date_time', 'end_date_time']]
        return (df_active_trips.end_date_time - df_active_trips.start_date_time).dt.total_seconds().sum()/3600

    @property
    def revenue_cost_ratio(self):
        return self._obj.revenue / self._obj.costs

    @property
    def profit(self):
        return self._obj.revenue - self._obj.costs

    @property
    def costs(self):
        return self._obj.costs

    @property
    def revenue(self):
        return self._obj.revenue

    def get_fleet_profit_ts(self, frequency='1H'):
        ts_profit = self.profit
        ts_profit.index = self._obj.start_date_time
        ts_profit = ts_profit.resample(frequency).sum()
        return ts_profit
