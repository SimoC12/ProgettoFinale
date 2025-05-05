package com.example.management.application;

import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@Service
public class GameManager {

    private int numeroCasuale = generaNumeroCasuale();
    private int tentativiGiocatore = 0;
    private String nomeGiocatore = "";
    private final List<Giocatore> classifica = new ArrayList<>();
    private final List<String> tentativiEffettuati = new ArrayList<>();
    private static final String CLASSIFICA_FILE = "src/main/resources/classifica.csv";
    private int tentativoCounter = 0;
    private boolean giocoTerminato = false; 

    @GetMapping("/")
    public String home(Model model) {
        caricaClassifica();
        preparaModello(model, nomeGiocatore, "Indovina un numero tra 1 e 100!", 0);
        return "index";
    }

    @PostMapping("/")
    public String tentativo(@RequestParam("nomeGiocatore") String nomeGiocatoreInput,
                            @RequestParam("numero") int numero,
                            Model model) {
        String messaggio = processaTentativo(nomeGiocatoreInput, numero);
        preparaModello(model, nomeGiocatore, messaggio, tentativiGiocatore);
        return "index";
    }

    @PostMapping("/gioca-ancora")
    public String giocaAncora(Model model) {
        resetGioco();
        preparaModello(model, "", "Indovina un numero casuale tra 1 e 100!", 0);
        return "index";
    }

    private void preparaModello(Model model, String nomeGiocatore, String messaggio, int tentativiGiocatore) {
        model.addAttribute("tentativiGiocatore", tentativiGiocatore);
        model.addAttribute("tentativiEffettuati", tentativiEffettuati);
        model.addAttribute("nomeGiocatore", nomeGiocatore);
        model.addAttribute("classifica", classifica);
        model.addAttribute("giocoTerminato", giocoTerminato); 
    }

    public void caricaClassifica() {
        classifica.clear();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("classifica.csv")))) {
            reader.lines().map(this::fromString).forEach(classifica::add);
        } catch (Exception e) {
            System.out.println("Classifica non trovata o vuota.");
        }
    }

    public void salvaClassifica() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CLASSIFICA_FILE))) {
            for (Giocatore giocatore : classifica) {
                writer.write(giocatore.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String processaTentativo(String nomeGiocatoreInput, int numero) {
        if (giocoTerminato) {
            return "Partita terminata! Premi 'Gioca Ancora' per ricominciare.";
        }

        if (nomeGiocatore.isEmpty()) {
            nomeGiocatore = nomeGiocatoreInput;
        }

        tentativiGiocatore++;
        tentativoCounter++;

        String messaggio;
        if (numero < numeroCasuale) {
            messaggio = "Tentativo " + tentativoCounter + ": " + numero + " -> Il numero da indovinare è più grande!";
        } else if (numero > numeroCasuale) {
            messaggio = "Tentativo " + tentativoCounter + ": " + numero + " -> Il numero da indovinare è più piccolo!";
        } else {
            messaggio = "Tentativo " + tentativoCounter + ": " + numero + " -> 🎉 Hai indovinato!";
            tentativiEffettuati.add(messaggio);
            return gestisciVittoria();
        }

        tentativiEffettuati.add(messaggio);
        return messaggio;
    }

    private String gestisciVittoria() {
        String messaggio = "Tentativo " + tentativoCounter + ": 🎉 Hai indovinato il numero " + numeroCasuale + " in " + tentativiGiocatore + " tentativi!";
        
        tentativiEffettuati.add(messaggio);
        
        classifica.add(new Giocatore(nomeGiocatore, tentativiGiocatore));
        classifica.sort(Comparator.comparingInt(Giocatore::getTentativi));
        salvaClassifica();

        giocoTerminato = true; // Imposto il flag per indicare che la partita è finita

        return messaggio;
    }

    public void resetGioco() {
        numeroCasuale = generaNumeroCasuale();
        tentativiGiocatore = 0;
        tentativoCounter = 0;
        nomeGiocatore = "";
        tentativiEffettuati.clear();
        giocoTerminato = false; // Reset del flag di fine partita
    }

    private int generaNumeroCasuale() {
        return (int) (Math.random() * 100) + 1;
    }

    private Giocatore fromString(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Linea vuota o null, impossibile creare un Giocatore.");
        }

        String[] parts = line.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Formato non valido per la linea: " + line);
        }

        try {
            int tentativi = Integer.parseInt(parts[1].trim());
            return new Giocatore(parts[0].trim(), tentativi);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Numero di tentativi non valido: " + parts[1], e);
        }
    }

    private static class Giocatore {
        private final String nome;
        private final int tentativi;

        public Giocatore(String nome, int tentativi) {
            this.nome = nome;
            this.tentativi = tentativi;
        }

        public String getNome() {
            return nome;
        }

        public int getTentativi() {
            return tentativi;
        }

        @Override
        public String toString() {
            return nome + "," + tentativi;
        }
    }
}
