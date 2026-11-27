package dev.aurora.connect
import org.junit.Test
import org.junit.Assert.*
class ModePolicyTest {
 @Test fun lanRanges() {
  assertTrue(privateLanRoute("192.168.0.0/16"))
  assertTrue(privateLanRoute("172.16.0.0/12"))
  assertFalse(privateLanRoute("172.16.0.0/11"))
  assertFalse(privateLanRoute("0.0.0.0/0"))
  assertFalse(privateLanRoute("192.168.0.0/15"))
  assertFalse(privateLanRoute("::/0"))
 }
}
