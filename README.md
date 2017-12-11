# vnfs-ML
Machine learning routines for Vehere Network Forensic System.

vnfs-ml contains routines for performing suspicious connections analyses on DNS data gathered from a network. These analyses consume collections of network events and produce lists of the events that are considered to be the least probable (most suspicious).

The suspicious connects analysis assigns to each record a score in the range 0 to 1, with smaller scores being viewed as more suspicious or anomalous than larger scores.

**Prepare Data For Input**
Whether suspicious connects is called by ml_ops.sh, data must be in the schema used by the suspicious connects analyses. Ingesting data via the Vnfs capture tools will store data in an appropriate schema.
