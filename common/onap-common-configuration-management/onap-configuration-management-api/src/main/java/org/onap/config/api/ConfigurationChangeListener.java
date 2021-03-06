package org.onap.config.api;

public interface ConfigurationChangeListener {

  public default void notify(String tenantId, String component, String key, Object oldValue,
                             Object newValue) {
  }

  public default void notify(String component, String key, Object oldValue, Object newValue) {
  }

  public default void notify(String key, Object oldValue, Object newValue) {
  }
}
