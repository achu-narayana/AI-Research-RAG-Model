import os
import time

from dotenv import load_dotenv
from openai import OpenAI


# ==================================================
# Load environment variables
# ==================================================

load_dotenv()

API_KEY = os.getenv("OPENROUTER_API_KEY")

if not API_KEY:
    raise RuntimeError(
        "OPENROUTER_API_KEY is not set in the .env file."
    )


# ==================================================
# OpenRouter client
# ==================================================

client = OpenAI(
    base_url="https://openrouter.ai/api/v1",
    api_key=API_KEY
)


# ==================================================
# Model
# ==================================================

MODEL = "qwen/qwen3.8-27b:free"


# ==================================================
# LLM helper
# ==================================================

def ask_llm(
        prompt: str,
        max_tokens: int = 2000,
        retries: int = 3
) -> str:

    last_error = None

    for attempt in range(1, retries + 1):

        try:

            print(
                f"    Sending request to OpenRouter "
                f"(attempt {attempt}/{retries})..."
            )

            response = client.chat.completions.create(
                model=MODEL,

                messages=[
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],

                temperature=0.2,

                max_tokens=max_tokens
            )


            # ------------------------------------------
            # Read response safely
            # ------------------------------------------

            if not response.choices:

                raise RuntimeError(
                    "OpenRouter returned no choices."
                )


            message = response.choices[0].message

            answer = message.content


            # ------------------------------------------
            # Handle empty response
            # ------------------------------------------

            if answer and answer.strip():

                print(
                    "    OpenRouter response received."
                )

                return answer.strip()


            # ------------------------------------------
            # Debug information
            # ------------------------------------------

            finish_reason = (
                response.choices[0].finish_reason
            )

            print(
                "    OpenRouter returned empty content."
            )

            print(
                "    Finish reason:",
                finish_reason
            )


            raise RuntimeError(
                "OpenRouter returned an empty response."
            )


        except Exception as e:

            last_error = e

            print(
                f"    OpenRouter attempt {attempt} failed:"
            )

            print(
                f"    {e}"
            )


            if attempt < retries:

                print(
                    "    Retrying in 2 seconds..."
                )

                time.sleep(2)


    raise RuntimeError(
        "OpenRouter failed after "
        f"{retries} attempts: {last_error}"
    )
# import os
# import time

# from dotenv import load_dotenv
# from openai import OpenAI


# load_dotenv()

# API_KEY = os.getenv("OPENROUTER_API_KEY")

# if not API_KEY:
#     raise RuntimeError(
#         "OPENROUTER_API_KEY is not set in the .env file."
#     )


# client = OpenAI(
#     base_url="https://openrouter.ai/api/v1",
#     api_key=API_KEY
# )


# MODEL = "nvidia/nemotron-3.5-lightning:free"


# def ask_llm(
#         prompt: str,
#         max_tokens: int = 2000,
#         retries: int = 3
# ) -> str:

#     last_error = None

#     for attempt in range(1, retries + 1):

#         try:

#             print(
#                 f"    Sending request to OpenRouter "
#                 f"(attempt {attempt}/{retries})..."
#             )

#             response = client.chat.completions.create(
#                 model=MODEL,

#                 messages=[
#                     {
#                         "role": "user",
#                         "content": prompt
#                     }
#                 ],

#                 temperature=0.2,
#                 max_tokens=max_tokens
#             )

#             if not response.choices:
#                 raise RuntimeError(
#                     "OpenRouter returned no choices."
#                 )

#             answer = response.choices[0].message.content

#             if answer and answer.strip():

#                 print(
#                     "    OpenRouter response received."
#                 )

#                 return answer.strip()

#             finish_reason = (
#                 response.choices[0].finish_reason
#             )

#             print(
#                 "    OpenRouter returned empty content."
#             )

#             print(
#                 "    Finish reason:",
#                 finish_reason
#             )

#             raise RuntimeError(
#                 "OpenRouter returned an empty response."
#             )

#         except Exception as e:

#             last_error = e

#             print(
#                 f"    OpenRouter attempt {attempt} failed:"
#             )

#             print(e)

#             if attempt < retries:

#                 print(
#                     "    Retrying in 5 seconds..."
#                 )

#                 time.sleep(5)

#     raise RuntimeError(
#         "OpenRouter failed after "
#         f"{retries} attempts: {last_error}"
#     )