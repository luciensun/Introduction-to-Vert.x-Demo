package io.vertx.blog.first;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.RestAssured;


public class MyRestIT {

	@BeforeClass
	public static void configureRestAssured() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = Integer.getInteger("http.port", 8080);
	}

	@AfterClass
	public static void unconfigureRestAssured() {
		RestAssured.reset();
	}

	@Test
	public void checkThatWeCanRetrieveIndividualProduct() {
		// Get the list of bottles, ensure it's a success and extract the first id.
		final int id = RestAssured.get("/api/whiskies").then().assertThat().statusCode(200).extract().jsonPath()
				.getInt("find { it.name=='Bowmore 15 Years Laimrig' }.id");
		// Now get the individual resource and check the content
		RestAssured.get("/api/whiskies/" + id).then().assertThat().statusCode(200).body("name", equalTo("Bowmore 15 Years Laimrig"))
				.body("origin", equalTo("Scotland, Islay")).body("id", equalTo(id));
	}
	
	@Test
	  public void checkWeCanAddAndDeleteAProduct() {
	    // Create a new bottle and retrieve the result (as a Whisky instance).
	    Whisky whisky = RestAssured.given()
	        .body("{\"name\":\"Jameson\", \"origin\":\"Ireland\"}").request().post("/api/whiskies").thenReturn().as(Whisky.class);
	    Assertions.assertThat(whisky.getName()).isEqualToIgnoringCase("Jameson");
	    Assertions.assertThat(whisky.getOrigin()).isEqualToIgnoringCase("Ireland");
	    Assertions.assertThat(whisky.getId()).isNotZero();

	    // Check that it has created an individual resource, and check the content.
	    RestAssured.get("/api/whiskies/" + whisky.getId()).then()
	        .assertThat()
	        .statusCode(200)
	        .body("name", equalTo("Jameson"))
	        .body("origin", equalTo("Ireland"))
	        .body("id", equalTo(whisky.getId()));

	    // Delete the bottle
	    RestAssured.delete("/api/whiskies/" + whisky.getId()).then().assertThat().statusCode(204);

	    // Check that the resrouce is not available anymore
	    RestAssured.get("/api/whiskies/" + whisky.getId()).then()
	        .assertThat()
	        .statusCode(404);
	}

}