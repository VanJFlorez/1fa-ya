package com.jcflorezv.draft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import redis.clients.jedis.Jedis;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringSessionJdbcApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class SessionControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private Jedis jedis;
    private TestRestTemplate testRestTemplate;
    private TestRestTemplate testRestTemplateWithAuth;    
    
    @Before
    public void clearRedisData() {
        
        testRestTemplate = new TestRestTemplate();
        testRestTemplateWithAuth = new TestRestTemplate("admin", "password");

        jedis = new Jedis("localhost", 6379);
        jedis.flushAll();
    }

    @Test
    public void testRedisIsEmpty() {
        Set<String> result = jedis.keys("*");
        assertEquals(0, result.size());
    }

    @Test
    public void testUnauthenticatedCantAccess() {
        ResponseEntity<String> result = testRestTemplate.getForEntity(getTestUrl(), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    public void testRedisControlsSession() {
        ResponseEntity<String> result = testRestTemplateWithAuth.getForEntity(getTestUrl(), String.class);
        assertEquals("hello admin", result.getBody()); // login worked

        Set<String> redisResult = jedis.keys("*");
        assertTrue(redisResult.size() > 0); // redis is populated with session data

        String sessionCookie = result.getHeaders().get("Set-Cookie").get(0).split(";")[0];
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionCookie);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        result = testRestTemplate.exchange(getTestUrl(), HttpMethod.GET, httpEntity, String.class);
        assertEquals("hello admin", result.getBody()); // access with session works worked

        jedis.flushAll(); // clear all keys in redis

        result = testRestTemplate.exchange(getTestUrl(), HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());// access denied after sessions are removed in redis
    }

    private String getTestUrl(){
        return "http://localhost:" + port;
    }
}