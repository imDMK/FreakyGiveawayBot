package me.dmk.app.configuration;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.javacord.api.entity.activity.ActivityType;

/**
 * Created by DMK on 29.03.2023
 */

@Getter
public class AppConfiguration extends OkaeriConfig {

    public String token = "MTA5MDc0MDMyNjExODk5ODA5OQ.GSi-Qb.Wg7vSp95RDURJKlVngZrXJKV8bcp3Uw0rGSyOE";

    public ActivityType activityType = ActivityType.LISTENING;
    public String activityName = "I'm just playing music.";

    public DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration();
}
