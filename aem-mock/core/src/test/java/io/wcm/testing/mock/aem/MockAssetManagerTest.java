/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.testing.mock.aem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamEvent;

import io.wcm.testing.mock.aem.context.TestAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;

public class MockAssetManagerTest {

  @Rule
  public AemContext context = TestAemContext.newAemContext();

  private DamEventHandler damEventHandler;

  @Before
  public void setUp() {
    damEventHandler = (DamEventHandler)context.registerService(EventHandler.class, new DamEventHandler());
  }

  @Test
  public void testCreateAsset() throws IOException {
    InputStream testImage = openTestAsset();
    String assetName = "myasset.gif";
    String mimeType = "image/gif";

    Asset asset = context.assetManager().createAsset(context.uniqueRoot().dam() + '/' + assetName, testImage, "image/gif", true);

    assertNotNull(asset);
    assertNotNull(asset.getOriginal().getStream());
    assertTrue(IOUtils.contentEquals(openTestAsset(), asset.getOriginal().getStream()));
    assertEquals(asset.getName(), assetName);
    assertEquals(asset.getMimeType(), mimeType);

    Optional<DamEvent> damEvent = damEventHandler.getLastEvent();
    assertTrue(damEvent.isPresent());
    assertEquals(DamEvent.Type.ASSET_CREATED, damEvent.get().getType());
    assertEquals(asset.getPath(), damEvent.get().getAssetPath());
  }

  private InputStream openTestAsset() {
    return getClass().getClassLoader().getResourceAsStream("sample-image.gif");
  }


  static final class DamEventHandler implements EventHandler {

    private final List<DamEvent> events = new ArrayList<>();

    @Override
    public void handleEvent(Event event) {
      if (StringUtils.equals(event.getTopic(), DamEvent.EVENT_TOPIC)) {
        events.add(DamEvent.fromEvent(event));
      }
    }

    public List<DamEvent> getEvents() {
      return this.events;
    }

    public Optional<DamEvent> getLastEvent() {
      if (this.events.isEmpty()) {
        return Optional.empty();
      }
      else {
        return Optional.of(events.get(events.size() - 1));
      }
    }

  }

}
