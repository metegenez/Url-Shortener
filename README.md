# Url-Shortener
Java based REST service.
- Service gets a link as an input and shorten it to 7 characters.
- When the shortened link is visited, it redirects to the original link.
	Example: http://localhost/axskadkaj → https://<domain>/abcde

- Each link has an /info endpoint to report the counts.
- Same links result in the same shortened URL. No duplicates.
- The user can also request a prefix if it’s available.
- Owner of the link can delete the URL if they want.
## Dependencies
- AWS Account
- AWS IAM user with [AmazonDynamoDBFullAccess](https://github.com/SummitRoute/aws_managed_policies/blob/master/policies/AmazonDynamoDBFullAccess)
- Spring Boot
- AWS DynamoDB

## How to Try
Entering the aws iam user credentials you created in the .env file is enough to launch the service. It will serve at localhost:8080.

## API 
You can access API documentation at http://localhost:8080/swagger-ui.html.



