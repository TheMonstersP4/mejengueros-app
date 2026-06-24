package example.data.local

class SqlDelightLaunchDataSource(database: AppDatabase) : LocalLaunchDataSource {
    private val queries = database.launchQueries

    override fun getAll(): List<Launch> =
        queries.selectAllLaunches { flightNumber, missionName, launchDateUTC, launchSuccess ->
            Launch(
                flightNumber = flightNumber.toInt(),
                missionName = missionName,
                launchDateUTC = launchDateUTC,
                launchSuccess = launchSuccess,
            )
        }.executeAsList()

    override fun replaceAll(launches: List<Launch>) {
        queries.transaction {
            queries.removeAllLaunches()
            launches.forEach { launch ->
                queries.insertLaunch(
                    flightNumber = launch.flightNumber.toLong(),
                    missionName = launch.missionName,
                    launchDateUTC = launch.launchDateUTC,
                    launchSuccess = launch.launchSuccess,
                )
            }
        }
    }
}
