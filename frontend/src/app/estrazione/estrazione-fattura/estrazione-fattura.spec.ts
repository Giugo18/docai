import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EstrazioneFattura } from './estrazione-fattura';

describe('EstrazioneFattura', () => {
  let component: EstrazioneFattura;
  let fixture: ComponentFixture<EstrazioneFattura>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EstrazioneFattura],
    }).compileComponents();

    fixture = TestBed.createComponent(EstrazioneFattura);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
