import requests
import random
import time
import logging
from multiprocessing import Pool
from functools import partial
from faker import Faker


logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('stress_test.log'),
        logging.StreamHandler()
    ]
)

fake = Faker()

ENDPOINTS = {
    "api_users": {
        "url": "http://127.0.0.1:8080/api/users/",
        "methods": {
            "GET": None,
            "POST": {
                "name": lambda: fake.name(),
                "email": lambda: fake.email()
            }
        }
    },
    "admin": {
        "url": "http://127.0.0.1:8080/admin/",
        "methods": {
            "GET": None
        }
    },
    "send": {
        "url": "http://127.0.0.1:8080/send/",
        "methods": {
            "POST": {
                "body": lambda: fake.sentence(),
                "content": lambda: fake.text()
            }
        }
    },
    "comment": {
        "url": "http://127.0.0.1:8080/comment/",
        "methods": {
            "POST": {
                "text": lambda: fake.text(),
                "author": lambda: fake.email()
            }
        }
    }
}


def generate_payload(method_spec):
    """Генерация тестовых данных для запроса"""
    if not method_spec:
        return None
    return {key: generator() for key, generator in method_spec.items()}


def test_endpoint(endpoint_name, endpoint_info):
    """Тестирование одного эндпоинта"""
    url = endpoint_info['url']
    methods = endpoint_info['methods']

    method = random.choice(list(methods.keys()))
    payload = generate_payload(methods[method])

    try:
        start_time = time.time()

        if method == "GET":
            response = requests.get(url)
        elif method == "POST":
            response = requests.post(url, json=payload)
        else:
            response = requests.get(url)

        duration = time.time() - start_time

        if response.status_code < 400:
#             logging.info(f"Success: {method} {url} - {response.status_code} - {duration:.2f}s")
            return True, duration
        else:
            logging.warning(f"Fail: {method} {url} - {response.status_code} - {duration:.2f}s")
            return False, duration

    except Exception as e:
        logging.error(f"Error: {method} {url} - {str(e)}")
        return False, 0


def worker(endpoint_names):
    """Рабочая функция для каждого процесса"""
    results = []
    for endpoint_name in endpoint_names:
        endpoint_info = ENDPOINTS[endpoint_name]
        success, duration = test_endpoint(endpoint_name, endpoint_info)
        results.append((endpoint_name, success, duration))

    return results


def run_stress_test(processes=10, requests_per_process=100):
    """Запуск стресс-теста"""
    endpoint_names = list(ENDPOINTS.keys())

    logging.info(f"Starting stress test with {processes} processes and {requests_per_process} requests each")
    logging.info("Testing endpoints:")
    for name, info in ENDPOINTS.items():
        logging.info(f"  {name}: {info['url']} ({', '.join(info['methods'].keys())})")

    start_time = time.time()

    with Pool(processes=processes) as pool:
        results = pool.map(worker, [endpoint_names] * requests_per_process)

    total_time = time.time() - start_time

    flat_results = [item for sublist in results for item in sublist]
    total_requests = len(flat_results)
    success_count = sum(1 for _, success, _ in flat_results if success)
    avg_duration = sum(duration for _, _, duration in flat_results) / total_requests

    endpoint_stats = {}
    for name in ENDPOINTS.keys():
        endpoint_results = [r for r in flat_results if r[0] == name]
        endpoint_total = len(endpoint_results)
        endpoint_success = sum(1 for _, success, _ in endpoint_results if success)
        endpoint_avg_time = sum(duration for _, _, duration in endpoint_results) / endpoint_total if endpoint_total > 0 else 0
        endpoint_stats[name] = {
            'total': endpoint_total,
            'success': endpoint_success,
            'success_rate': (endpoint_success / endpoint_total) * 100 if endpoint_total > 0 else 0,
            'avg_time': endpoint_avg_time
        }

    logging.info("\n=== Test Results ===")
    logging.info(f"Total requests: {total_requests}")
    logging.info(f"Total time: {total_time:.3f}s")
    logging.info(f"RPS: {total_requests / total_time:.3f}")
    logging.info(f"Successful: {success_count} ({(success_count/total_requests)*100:.1f}%)")
    logging.info(f"Average response time: {avg_duration:.3f}s")

    logging.info("\n=== Endpoint Details ===")
    for name, stats in endpoint_stats.items():
        logging.info(
            f"{name}: {stats['total']} requests, "
            f"{stats['success']} successful ({stats['success_rate']:.1f}%), "
            f"avg time {stats['avg_time']:.3f}s"
        )


if __name__ == "__main__":
    run_stress_test(processes=10, requests_per_process=800)