package com.example.discordbot.models;

import lombok.Data;
import org.mongojack.Id;
import org.bson.types.ObjectId;

@Data
public class Application {
    @Id
    private ObjectId id;

    private String userId;
    private String name;
    private String skills;
    private String languages;
    private String showcase;
    private String info;
    private String channelId;
    private boolean reviewed = true;
    private long timestamp;
}
