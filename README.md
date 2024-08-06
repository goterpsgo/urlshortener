# urlshortener

Demo app to showcase browser redirection \
This app was designed to run in a Unix-like environment.

# 1. Install packages

## Python
- Install `pipenv`
  - `$ pip install --user pipenv`
- Start shell
  - `$ pipenv shell`
- Install requirements
  - `$ pipenv install`

# 2. Start application

## Python API
- Start application
  - `$ [python] ./app.py`

# 3. Testing

## Test individual API endpoints
- Do the following in VS Code
  - Install REST client extension
    - https://marketplace.visualstudio.com/items?itemName=humao.rest-client
  - In `server/samples/api_test.http` click on *Send Request* to make API call
