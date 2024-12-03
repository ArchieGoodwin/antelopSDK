package com.reactnative.antelop;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.google.firebase.messaging.RemoteMessage;

import java.util.Iterator;
import java.util.Map;

public class RemoteMessageBuilder {
    private final Bundle zza = new Bundle();
    private final Map<String, String> zzb = new ArrayMap();

    public RemoteMessageBuilder() {
    }

    @NonNull
    public RemoteMessage build() {
        Bundle var1 = new Bundle();
        Iterator var2 = this.zzb.entrySet().iterator();

        while(var2.hasNext()) {
            Map.Entry var3 = (Map.Entry)var2.next();
            var1.putString((String)var3.getKey(), (String)var3.getValue());
        }

        var1.putAll(this.zza);
        return new RemoteMessage(var1);
    }

    @NonNull
    public RemoteMessageBuilder addData(@NonNull String var1, @Nullable String var2) {
        this.zzb.put(var1, var2);
        return this;
    }

    @NonNull
    public RemoteMessageBuilder setData(@NonNull Map<String, String> var1) {
        this.zzb.clear();
        this.zzb.putAll(var1);
        return this;
    }

    @NonNull
    public RemoteMessageBuilder clearData() {
        this.zzb.clear();
        return this;
    }

    @NonNull
    public RemoteMessageBuilder setMessageId(@NonNull String var1) {
        this.zza.putString("google.message_id", var1);
        return this;
    }

    @NonNull
    public RemoteMessageBuilder setMessageType(@Nullable String var1) {
        this.zza.putString("message_type", var1);
        return this;
    }

    @NonNull
    public RemoteMessageBuilder setTtl(@IntRange(from = 0L,to = 86400L) int var1) {
        this.zza.putString("google.ttl", String.valueOf(var1));
        return this;
    }

    @NonNull
    public RemoteMessageBuilder setCollapseKey(@Nullable String var1) {
        this.zza.putString("collapse_key", var1);
        return this;
    }

    @NonNull
    public RemoteMessageBuilder setFrom(@Nullable String var1) {
        this.zza.putString("from", var1);
        return this;
    }
}
