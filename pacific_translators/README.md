# demo-engineering-translators
Translate feed and pixels for Bloomreach demo accounts

PacificHome and PacificSupply initially used actual merchant catalogs
    pacifichome: worldmarket
    pacificsupply: partsource

    - each of these required pixel, api and catalog transformations
        - BR stores such logs in protobuf format, called api and pixel 'logs'
        - Analytics team has built (and uses) java libraries to read/write such logs
        - The translators used those libraries to read then process then write logs
            - pixel logs for 30 days were generated and saved to S3
            - the Analytics pipelines processed on a round-robin basis over a 30-day period
    - catalog files were also translated and then used to index into Bloomreach accts
    - each transformation was also anonymized as much as possible

Later, for pacifichome, both catalog and pixels were generated independently
    - catalog generated using AI (openAI), including description, title, images, prices, ...
    - pixels generated via trafficGenerator

For PacificSupply, the catalog has been translated (as explained above)
    - pixels still remain the same as before (ie, not generated independently)
    