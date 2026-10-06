class ServiceError(Exception):
    """
    An error with an HTTP status code and a message that is
    safe to show to the user. main.py turns it into
    {"detail": message} with the given status code.
    """

    def __init__(self, status_code: int, message: str):
        super().__init__(message)
        self.status_code = status_code
        self.message = message
