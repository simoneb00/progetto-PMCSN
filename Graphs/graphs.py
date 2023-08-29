import matplotlib.pyplot as plt
import numpy as np


def read_data(file_path):
    i = 1
    with open(file_path, 'r') as file:
        lines = file.readlines()
        data = [line.strip().split() for line in lines]
        x = ['(10, 20, 8, 20)',
             '(11, 21, 8, 20)',
             '(12, 22, 8, 20)',
             '(12, 22, 8, 21)',
             '(13, 23, 8, 21)',
             '(13, 25, 8, 21)',
             '(13, 25, 8, 23)',
             '(13, 25, 8, 24)']
        y = [float(entry[0]) for entry in data]
        print(y)
    return x, y


def plot(file_path, statistic, node):
    try:
        with open(file_path, 'r') as file:
            response_times = [float(line.strip()) for line in file.readlines()]

            # Creazione dell'asse degli indici
            indices = [(i + 1) * 1024 * 7 for i in range(len(response_times))]

            # Creazione del grafico
            plt.figure(figsize=(10, 6))
            plt.plot(indices, response_times, marker='o')
            plt.title(f"{statistic} - {node}")
            plt.xlabel('Numero di Utenti')
            plt.ylabel(statistic)
            plt.grid(True)
            plt.show()

    except FileNotFoundError:
        print("File non trovato.")
    except Exception as e:
        print(f"Si è verificato un errore: {e}")


if __name__ == "__main__":

    response_time = "Tempo di Risposta"
    utilization = "Utilizzazione"
    ticket_check = "Controllo biglietti"
    first_perquisition = "Prima perquisizione"
    turnstiles = "Tornelli"
    second_perquisition = "Seconda perquisizione"

    """
    plot("../batch_reports/response_times_ticket_check.dat", response_time, ticket_check)
    plot("../batch_reports/response_times_first_perquisition.dat", response_time, first_perquisition)
    plot("../batch_reports/response_times_turnstiles.dat", response_time, turnstiles)
    plot("../batch_reports/response_times_second_perquisition.dat", response_time, second_perquisition)
    plot("../batch_reports/utilizations_ticket_check.dat", utilization, ticket_check)
    plot("../batch_reports/utilizations_first_perquisition.dat", utilization, first_perquisition)
    plot("../batch_reports/utilizations_turnstiles.dat", utilization, turnstiles)
    plot("../batch_reports/utilizations_second_perquisition.dat", utilization, second_perquisition)
    """

    file_paths = ['../experiments/ticket_check_rt.dat', '../experiments/first_perquisition_rt.dat', '../experiments/turnstiles_rt.dat', '../experiments/second_perquisition_rt.dat', '../experiments/total_rt.dat']

    # Read data from each file
    data = [read_data(file_path) for file_path in file_paths]

    # Create a new figure and axis
    plt.figure(figsize=(7, 12))
    plt.xlabel('Configurazioni')
    plt.ylabel('Tempi di Risposta')

    nodes = ['Controllo biglietti', 'Prima perquisizione', 'Tornelli', 'Seconda perquisizione', 'Tempo di risposta totale']

    # Plot data from each file
    for i, (x, y) in enumerate(data, start=1):
        label = f'{nodes[i-1]}'
        plt.plot(x, y, label=label)

    x = [0, 1, 2, 3, 4, 5, 6, 7]
    label = 'Obiettivo (t.d.r. = 20 min)'
    plt.plot(x, np.full(8, 1200), color='black', label=label)


    plt.xticks(rotation=45, ha='right')

    # Add legend
    plt.legend()

    # Display the plot
    plt.show()
