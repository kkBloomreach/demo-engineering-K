# pathname relative to the '-d' parameter in command line (eg, './data')
INPUT_ACCOUNT_CONFIGS_PATHNAME = 'input/account_configs.json'

# Following according to Bloomreach document
VALID_API_STATUS_VALUES = {'creating', 'queued', 'running', 'success', 'failed', 'skipped', 'killed'}
VALID_API_STATUS_SUCCESS = 'success'
VALID_API_STATUS_FAILED  = 'failed'
VALID_API_STATUS_KILLED  = 'killed'

# time between repeat check-status api calls
MIN_TIME_BEFORE_RECHECK_STATUS = 5
MAX_TRIES_TO_CHECK_STATUS = 5


