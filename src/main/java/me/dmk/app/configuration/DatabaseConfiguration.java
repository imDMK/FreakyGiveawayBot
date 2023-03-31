package me.dmk.app.configuration;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;

/**
 * Created by DMK on 29.03.2023
 */

@Getter
public class DatabaseConfiguration extends OkaeriConfig {

    public boolean authentication = false;

    public String userName = "";
    public String password = "";
    public String hostName = "localhost";
    public String databaseName = "discordclient";

    public int port = 27017;
}
