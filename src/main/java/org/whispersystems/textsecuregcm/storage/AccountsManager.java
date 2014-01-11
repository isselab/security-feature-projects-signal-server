/**
 * Copyright (C) 2013 Open WhisperSystems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.whispersystems.textsecuregcm.storage;


import com.google.common.base.Optional;
import net.spy.memcached.MemcachedClient;
import org.whispersystems.textsecuregcm.entities.ClientContact;
import org.whispersystems.textsecuregcm.util.Pair;
import org.whispersystems.textsecuregcm.util.Util;
import sun.util.logging.resources.logging_zh_CN;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccountsManager {

  private final Accounts         accounts;
  private final MemcachedClient  memcachedClient;
  private final DirectoryManager directory;

  public AccountsManager(Accounts accounts,
                         DirectoryManager directory,
                         MemcachedClient memcachedClient)
  {
    this.accounts        = accounts;
    this.directory       = directory;
    this.memcachedClient = memcachedClient;
  }

  public long getCount() {
    return accounts.getNumberCount();
  }

  public List<Device> getAllMasterDevices(int offset, int length) {
    return accounts.getAllMasterDevices(offset, length);
  }

  public Iterator<Device> getAllMasterDevices() {
    return accounts.getAllMasterDevices();
  }

  /** Creates a new Account (WITH ONE DEVICE), clearing all existing devices on the given number */
  public void create(Account account) {
    Device device = account.getDevices().iterator().next();
    long id = accounts.insertClearingNumber(device);
    device.setId(id);

    if (memcachedClient != null) {
      memcachedClient.set(getKey(device.getNumber(), device.getDeviceId()), 0, device);
    }

    updateDirectory(device);
  }

  /** Creates a new Device for an existing Account */
  public void provisionDevice(Device device) {
    long id = accounts.insert(device);
    device.setId(id);

    if (memcachedClient != null) {
      memcachedClient.set(getKey(device.getNumber(), device.getDeviceId()), 0, device);
    }

    updateDirectory(device);
  }

  public void update(Device device) {
    if (memcachedClient != null) {
      memcachedClient.set(getKey(device.getNumber(), device.getDeviceId()), 0, device);
    }

    accounts.update(device);
    updateDirectory(device);
  }

  public Optional<Device> get(String number, long deviceId) {
    Device device = null;

    if (memcachedClient != null) {
      device = (Device)memcachedClient.get(getKey(number, deviceId));
    }

    if (device == null) {
      device = accounts.get(number, deviceId);

      if (device != null && memcachedClient != null) {
        memcachedClient.set(getKey(number, deviceId), 0, device);
      }
    }

    if (device != null) return Optional.of(device);
    else                 return Optional.absent();
  }

  public Optional<Account> getAccount(String number) {
    List<Device> devices = accounts.getAllByNumber(number);
    if (devices.isEmpty())
      return Optional.absent();
    return Optional.of(new Account(number, devices.get(0).getSupportsSms(), devices));
  }

  private Map<String, Account> getAllAccounts(Set<String> numbers) {
    //TODO: ONE QUERY
    Map<String, Account> result = new HashMap<>();
    for (String number : numbers) {
      Optional<Account> account = getAccount(number);
      if (account.isPresent())
        result.put(number, account.get());
    }
    return result;
  }

  public Pair<Map<String, Account>, List<String>> getAccountsForDevices(Map<String, Set<Long>> destinations) {
    List<String> numbersMissingDevices = new LinkedList<>();
    Map<String, Account> localAccounts = getAllAccounts(destinations.keySet());

    for (String number : destinations.keySet()) {
      if (localAccounts.get(number) == null)
        numbersMissingDevices.add(number);
    }

    Iterator<Account> localAccountIterator = localAccounts.values().iterator();
    while (localAccountIterator.hasNext()) {
      Account account = localAccountIterator.next();
      if (!account.hasAllDeviceIds(destinations.get(account.getNumber()))) {
        numbersMissingDevices.add(account.getNumber());
        localAccountIterator.remove();
      }
    }

    return new Pair<>(localAccounts, numbersMissingDevices);
  }

  private void updateDirectory(Device device) {
    if (device.getDeviceId() != 1)
      return;

    if (device.isActive()) {
      byte[]        token         = Util.getContactToken(device.getNumber());
      ClientContact clientContact = new ClientContact(token, null, device.getSupportsSms());
      directory.add(clientContact);
    } else {
      directory.remove(device.getNumber());
    }
  }

  private String getKey(String number, long accountId) {
    return Device.class.getSimpleName() + Device.MEMCACHE_VERION + number + accountId;
  }
}
