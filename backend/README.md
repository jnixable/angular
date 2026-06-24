## Decisions 
1. Deposits and Withdrawals allowed only in EUR. In case of SWIFT transfer we will need to create a separate controller with its own models and requirements.
2. No lombok, better to use Kotlin ;)
3. it's nice to use resilience4j for 3rd party calls (when we have some retry, rate limits etc) but it's skipped here for simplicity
4. Distributed lock - mentioned in code comments. Skipped for simplicity