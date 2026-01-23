### Global Update Notes and Global TODOs

#### application.properties

* TODO: Determine session idle timeout when known add to `server.servlet.session.timeout`.  Suggest pretty large number for non-prod profiles, but prod should a smaller number.  Need to know if the frontend has a /invalidSession URL, if not need to implement.  Then need to configure security to redirect to that page.  Also need to add to the list of `permitAll()` urls.
