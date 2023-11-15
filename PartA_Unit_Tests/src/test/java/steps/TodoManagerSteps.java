package steps;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import io.cucumber.java.After;

public class TodoManagerSteps {
    private Response response;
    private String endpoint;
    private Process apiProcess;
    private String projectId;

    @Given("the Todo Manager API is running")
    public void apiRunning() {
        RestAssured.baseURI = "http://localhost:4567";
        if (!isApiRunning()) {
            startApi();
        } else {
            try {      
                given().when().get("/shutdown");
            } catch (Exception e) { } 
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            startApi();
        }
    }

    private boolean isApiRunning() {
        try {
            URL url = new URL("http://localhost:4567/todos");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private void startApi() {
        try {
            apiProcess = new ProcessBuilder("java", "-jar", "runTodoManagerRestAPI-1.5.5.jar").start();
            Thread.sleep(300);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Given("a project with the name {string} already exists")
    public void projectExists(String name) {
        endpoint = "/projects"; // Updating the endpoint variable
        String projectJson = "{\"title\": \"" + name + "\", \"description\": \"Autogenerated project\"}";
        Response postResponse = given()
                                  .contentType("application/json")
                                  .body(projectJson)
                                  .when()
                                  .post(endpoint)
                                  .then()
                                  .statusCode(201)
                                  .extract()
                                  .response();
        
        projectId = postResponse.path("id"); // Assuming 'id' is the field in the JSON response containing the project ID
    }

    @Given("no project exists")
    public void noProjectExists() {
        Response response = given().when().get("/projects");
        List<Integer> projectIds = response.jsonPath().getList("projects.id", Integer.class);
    
        for (Integer id : projectIds) {
            given().when().delete("/projects/" + id);
        }
    }

    @When("I send a POST request to {string} with name {string} and description {string}")
    public void postProject(String endpoint, String name, String description) {
        this.endpoint = endpoint;
        String projectJson = "{\"title\": \"" + name + "\", \"description\": \"" + description + "\"}";
        response = given().contentType("application/json").body(projectJson).when().post(this.endpoint);
    }

    @When("I send a POST request to {string} with ID {string} and name {string} and description {string}")
    public void postProjectWithId(String endpoint, String id, String name, String description) {
        String projectJson = "{\"id\": \"" + id + "\", \"title\": \"" + name + "\", \"description\": \"" + description + "\"}";
        response = given().contentType("application/json").body(projectJson).when().post(endpoint);
    }

    @When("I send a POST request to {string} without a name")
    public void postProjectWithoutName(String endpoint) {
        this.endpoint = endpoint;
        String projectJson = "{\"description\": \"Project without a name\"}";
        response = given().contentType("application/json").body(projectJson).when().post(this.endpoint);
    }

    @When("I send a GET request to {string}")
    public void sendGetRequest(String endpoint) {
        response = given().when().get(endpoint);
    }

    @When("I send a GET request to {string} with filter {string}")
    public void sendGetRequestWithFilter(String endpoint, String filter) {
        response = given().queryParam("title", filter).when().get(endpoint);
    }

    @When("I send a PUT request to {string} with ID {string}, new name {string} and description {string}")
    public void sendPutRequestWithIdNameAndDescription(String endpoint, String id, String newName, String description) {
        String projectJson = "{\"title\": \"" + newName + "\", \"description\": \"" + description + "\"}";
        response = given()
                       .contentType("application/json")
                       .body(projectJson)
                       .when()
                       .put("/projects/" + id);
    }    

    @When("I send a PUT request to {string} with ID {string} with active {string}")
    public void sendPutRequestWithActive(String endpoint, String id, String is_active) {
        String projectJson = "\"active\": \"" + is_active + "\"}";
        response = given()
                       .contentType("application/json")
                       .body(projectJson)
                       .when()
                       .put("/projects/" + id);
    }   
    
    @Then("I should receive a response with status code {int}")
    public void checkStatusCode(int statusCode) {
        response.then().statusCode(statusCode);
    }

    @Then("the response should contain a project with name {string} and description {string}")
    public void responseContainsProject(String name, String description) {
        response.then().body("title", equalTo(name)).body("description", equalTo(description));
    }

    @Then("the response should contain the error message {string}")
    public void responseShouldContainErrorMessage(String errorMessage) {
        String responseBody = response.asString();
        assertTrue(responseBody.contains(errorMessage));
    }

    @Then("the response should contain a project with name {string}")
    public void responseContainsProjectWithName(String expectedName) {
        String responseBody = response.asString();
        assertTrue(responseBody.contains(expectedName));
    }

    // get all categories
    @When("I send a GET request to {string} with Accept header {string}")
    public void sendGetRequestWithAcceptHeader(String endpoint, String acceptHeader) {
        response = given().header("Accept", acceptHeader).when().get(endpoint);
    }

    @When("I send a GET request to an invalid endpoint {string}")
    public void sendInvalidGetRequest(String endpoint) {
        response = given().when().get(endpoint);
    }

    @Then("the response should contain a list of categories in JSON format")
    public void checkResponseContainsCategoriesInJSON() {
        response.then().body("size()", greaterThan(0)).and().contentType(equalTo("application/json"));
    }

    @Then("the response should contain a list of categories in XML format")
    public void checkResponseContainsCategoriesInXML() {
        response.then().body("category", notNullValue()).and().contentType(equalTo("application/xml"));
    }

    @When("I send a DELETE request with ID {string}")
    public void sendDeleteRequest(String id) {
        response = given().when().delete("/projects/" + id);
    }

    @When("I send a DELETE request to newly created project")
    public void sendDeleteRequestForLastCreatedProject() {
        response = given().when().delete("/projects/" + projectId);
    }

    @And("the project with ID {string} is marked as inactive")
    public void markProjectAsInactive(String id) {
        String projectJson = "{\"active\": \"false\"}";
        given().contentType("application/json")
            .body(projectJson)
            .when()
            .put("/projects/" + id);
    }

    @Then("the response should contain a list of projects with {string}")
    public void responseContainsListOfProjectsWith(String projectName) {
        response.then().body("projects.title", hasItem(projectName));
    }    
    
    @Then("the response should contain an empty list of projects")
    public void responseContainsEmptyListOfProjects() {
        response.then().body("projects", hasSize(0));
    }

    //create new category
    @When("I send a POST request to {string} with the following details:")
    public void postCategory(String endpoint, Map<String, String> categoryDetails) {
        this.endpoint = endpoint;
        String categoryJson = String.format("{\"title\": \"%s\", \"description\": \"%s\"}",
                categoryDetails.get("title"),
                categoryDetails.get("description"));
        response = given().contentType("application/json").body(categoryJson).when().post(this.endpoint);
    }

    @When("I send a POST request to {string} without a title")
    public void postCategoryWithoutTitle(String endpoint) {
        String categoryJson = "{ \"description\": \"Category without a title\" }";
        response = given().contentType("application/json").body(categoryJson).when().post(endpoint);
    }


    @Then("the response should contain the created category details")
    public void checkResponseContainsCategoryDetails() {
        response.then().body("title", notNullValue()).body("description", notNullValue());
    }

    @Then("the project with name {string} should no longer exist")
    public void the_project_with_name_should_no_longer_exist(String s) {
        // Write code here that turns the phrase above into concrete actions
    }

    //get specific category
    @When("I send a GET request to {string} with ID {string} and with Accept header {string}")
    public void getCategoryById(String endpoint, String categoryId, String acceptHeader) {
        this.endpoint = endpoint.replace("{id}", categoryId);
        response = given().header("Accept", acceptHeader).when().get(this.endpoint);
    }

    @Then("the response should contain the category details in JSON format")
    public void checkResponseContainsCategoryDetailsJson() {
        response.then().body("size()", greaterThan(0)).and().contentType(equalTo("application/json"));

    }

    @Then("the response should contain the category details in XML format")
    public void checkResponseContainsCategoryDetailsXml() {
        response.then().contentType("application/xml").and().body("category.title", notNullValue())
                .body("category.description", notNullValue());
    }

    @When("I send a GET request to {string} with ID {string}")
    public void getCategoryByNonExistentId(String endpoint, String categoryId) {
        this.endpoint = endpoint.replace("{id}", categoryId);
        response = given().when().get(this.endpoint);
    }

    //update category
    @When("I send a PUT request with ID {string} and request body containing new title {string} and new description {string}")
    public void sendPutRequestWithNewTitleAndDescription(String categoryId, String newTitle, String newDescription) {
        String requestBody = "{\"title\":\"" + newTitle + "\",\"description\":\"" + newDescription + "\"}";
        response = RestAssured.given().header("Content-Type", "application/json").body(requestBody)
                .put("/categories/" + categoryId);
    }

    @When("I send a PUT request with ID {string} and request body containing modified title {string} and new description {string}")
    public void sendPutRequestWithModifiedTitleAndDescription(String categoryId, String modifiedTitle, String newDescription) {
        String requestBody = "{\"title\":\"" + modifiedTitle + "\",\"description\":\"" + newDescription + "\"}";
        response = RestAssured.given().header("Content-Type", "application/json").body(requestBody)
                .put("/categories/" + categoryId);
    }

    //delete category
    @When("I make a DELETE request with ID {string}")
    public void deleteCategoryById(String categoryId) {
        response = RestAssured.delete("/categories/" + categoryId);
    }

    @Then("I verify that the category with ID {string} no longer exists in the system by sending a GET request and receiving a response with status code 404")
    public void verifyCategoryDoesNotExist(String deletedCategoryId) {
        // Ensure that the category with the deleted ID no longer exists
        Response getResponse = RestAssured.get("/categories/" + deletedCategoryId);
        getResponse.then().statusCode(404);
    }

    private void sendRequestInterop(String method, String endpoint, String Id1, String Id2)
    {
        this.endpoint = endpoint;
        if (method.equals("POST"))
        {
            String projectJson = "{id : \""+Id2+"\"}";
            response = given().contentType("application/json").body(projectJson).pathParam("id", Id1).when().post(this.endpoint);

        } else if (method.equals("DELETE"))
        {
            response = given().contentType("application/json").pathParam("id", Id2).pathParam("id2", Id1).when().delete(this.endpoint);
        }
    }
    
    @Given("the user sent a {string} request {string} to category {string} to project {string}")
    public void givenSendPostRequestAddCategoryProject(String method, String endpoint, String categoryId, String projectId)
    {
        sendRequestInterop(method, endpoint, categoryId, projectId);
    }
    @When("the user sends a {string} request {string} to category {string} to project {string}")
    public void whenSendPostRequestAddCategoryProject(String method, String endpoint, String categoryId, String projectId)
    {
        sendRequestInterop( method,  endpoint,  categoryId,  projectId);
    }

    @When("the user sends a {string} request {string} to todo {string} to category {string}")
    public void whenSendPostRequestAddTodoCategory(String method, String endpoint, String categoryId, String projectId)
    {
        sendRequestInterop( method,  endpoint,  categoryId,  projectId);
    }
    @When("the user sends a {string} request {string} to todo {string} to project {string}")
    public void whenSendPostRequestAddTodoProject(String method, String endpoint, String categoryId, String projectId)
    {
        sendRequestInterop( method,  endpoint,  categoryId,  projectId);
    }
}
