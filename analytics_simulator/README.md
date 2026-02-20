# demo-engineering-analytics-simulator and data-generator

Analytics (pixel, api) simulator to generate Bloomreach analytics data

data-generator:
    This generator reads a list of pre-defined query terms and then generates .json files
    which are then used by the analytics simulator
    
    The simulator needs/uses data files (eg, query terms, suggest terms, ...). 
    Previously we edited those files manually. However, as that process became too
    complicated, we wrote this Generator and simplify that process.
    
    Needless to say, the data (and the format) of the generated files must match
    the ones expected by the simulator

simulator:
    Uses pre-created data (from source/.json) and generates pixel traffic.
    Uses java libraries created/mentained by Analytics engr team to write pixel logs in protobufs

