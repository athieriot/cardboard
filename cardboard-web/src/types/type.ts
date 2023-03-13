export type CardId = number
export type PlayerId = string

export interface Card {
    name: string
    set: string
    numberInSet: number
    abilities: [[number, string]]
}

export type ZoneEntry = [CardId, Card]
export type Zone = [ZoneEntry?]